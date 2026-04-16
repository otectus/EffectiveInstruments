# Effective Instruments v1.2.0 — Full Technical & Design Review

*Review date: 2026-03-26*
*Reviewed commit: `b1dfaaf` (Effective Instruments v1.2.0: Instrument-specific auras)*
*Scope: All source code, assets, configs, documentation, tests, and build setup*

---

## Executive Summary

Effective Instruments is a well-structured, focused Forge 1.20.1 mod that adds a configurable aura buff system to musical instruments. The codebase is small (~1,300 LOC across 22 main Java files), readable, and follows standard Forge patterns. Key architectural strengths:

- **Server-authoritative**: All effect application is validated server-side; the client only drives UI selection.
- **Data-driven**: Aura presets and instrument mappings are externalized to JSON config files with hot-reload support.
- **Clean separation**: Client/server code, networking, config, and aura logic are well-separated into focused packages.
- **Defensive design**: Stale aura cleanup on reload, STRONGEST_WINS effect policy, graceful handling of missing/disabled auras.

The mod is functionally complete for its scope. This review identifies 39 findings across correctness, performance, security, UX, balance, code quality, and build concerns, organized by severity with actionable recommendations.

---

## Table of Contents

1. [Critical: Bugs & Correctness Issues](#1-critical-bugs--correctness-issues)
2. [High: Security & Exploitability](#2-high-security--exploitability)
3. [High: Performance](#3-high-performance)
4. [Moderate: Architecture & Design](#4-moderate-architecture--design)
5. [Moderate: UI/UX](#5-moderate-uiux)
6. [Balance & Gameplay](#6-balance--gameplay)
7. [Content Consistency](#7-content-consistency)
8. [Code Quality](#8-code-quality)
9. [Documentation](#9-documentation)
10. [Build & CI](#10-build--ci)
11. [Compatibility](#11-compatibility)
12. [Prioritized Action Items](#12-prioritized-action-items)

---

## 1. Critical: Bugs & Correctness Issues

### 1.1 Player logout does not clean up effects on buffed targets

**File:** `AuraManager.java:111-113`, `InstrumentStateHandler.java:37-39`

`onPlayerLogout()` calls `PLAYER_STATES.remove(playerId)` without first calling `clearPreviousAuraEffects()`. If the `InstrumentOpenStateChangedEvent(isOpen=false)` fires before the logout event, effects are cleaned up by `onInstrumentClose()`. However, in abrupt disconnection scenarios (network drop, server kick), the close event may not fire, leaving aura effects on buffed entities to expire naturally over up to 260 ticks (13 seconds).

**Recommendation:** Call `onAuraSwitch(player)` inside `onPlayerLogout()` before removing the state. The `InstrumentStateHandler` currently passes only the UUID — change it to pass the `ServerPlayer` entity (available from `event.getEntity()`):

```java
// InstrumentStateHandler.java
@SubscribeEvent
public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer sp) {
        AuraManager.onAuraSwitch(sp);
    }
    AuraManager.onPlayerLogout(event.getEntity().getUUID());
}
```

### 1.2 Dimension change cleanup targets the wrong level

**File:** `AuraManager.java:256-287`, `InstrumentStateHandler.java:42-45`

When `PlayerChangedDimensionEvent` fires, the player has already moved to the new dimension. `clearPreviousAuraEffects()` calls `musician.serverLevel()` which returns the **new** level, then tries `level.getEntity(entry.getKey())` for entities that exist in the **old** level. All entity lookups fail silently, so no effects are actually cleaned up. Buffed entities in the old dimension retain the aura effects until they expire naturally.

**Recommendation:** Accept this as a known limitation (effects expire within 13 seconds) and add a code comment documenting it. Alternatively, resolve the old level via `event.getFrom()`:

```java
ServerLevel oldLevel = server.getLevel(event.getFrom());
```

Then pass it to a cleanup overload that uses the specified level instead of `musician.serverLevel()`.

### 1.3 Effect cleanup heuristic can incorrectly strip third-party effects

**File:** `AuraManager.java:280`

The condition `current.isAmbient() && current.getAmplifier() == ourAmplifier` matches any ambient effect at the same amplifier regardless of source. If a beacon provides Resistance I (ambient) and Warcry Cadence also provides Resistance I (ambient), switching away from Warcry Cadence would strip the beacon's Resistance I.

**Recommendation:** This is difficult to solve perfectly without a custom effect tracking system. The simplest mitigation is to also check the effect's remaining duration — aura effects have a known duration (160-260 ticks), so effects with much longer remaining durations are likely from other sources. Alternatively, document this as a known limitation.

### 1.4 `onInstrumentOpen` vs `onInstrumentOpenWithId` race window

**File:** `AuraManager.java:77-99`

The `InstrumentOpenStateChangedEvent` calls `onInstrumentOpen(player)` (no instrument ID), and the C2S packet calls `onInstrumentOpenWithId(player, instrumentId)`. Both set `instrumentOpen = true`. If the event fires before the packet, there's a brief window where the instrument is "open" with no ID.

The `NoteActivityHandler` fallback (lines 25-30) catches this case — if a note is played and no instrument ID is set, it calls `onInstrumentOpenWithId()` using the note's sound metadata. This is a good defensive design, but it means the first few notes may be played without an aura if the packet is delayed.

**Recommendation:** This is acceptable as-is. The fallback is robust. Add a brief comment explaining the design intent.

### 1.5 `affectedTargets` uses entity network ID as map key

**File:** `AuraManager.java:37, 160, 269`

`entity.getId()` returns the entity's network/session ID. If an entity unloads and a new entity gets the same ID, effects could be tracked against the wrong entity. In practice, within a single play session in a single dimension, IDs are stable. The real risk is that `clearPreviousAuraEffects` calls `level.getEntity(networkId)` — if the original entity unloaded, the lookup returns null and the effect is not cleaned up (fails silently).

**Recommendation:** Acceptable as-is for the current scope. The effects expire naturally within 13 seconds. If longer-duration effects are ever added, consider using `UUID` as the key and storing weak references to entities.

---

## 2. High: Security & Exploitability

### 2.1 No packet rate limiting

**File:** `SelectAuraC2SPacket.java:30-68`, `InstrumentOpenC2SPacket.java:26-35`

A malicious client can spam `SelectAuraC2SPacket` rapidly. Each invocation calls `onAuraSwitch()` which iterates all tracked targets. With many tracked targets and rapid switching, this creates unnecessary server load. Similarly, `InstrumentOpenC2SPacket` can be spammed to trigger auto-selection logic repeatedly.

**Recommendation:** Add a cooldown per player. A `lastPacketTime` field in `PlayerAuraState` with a minimum interval of ~250ms (5 ticks) would prevent abuse without affecting legitimate use:

```java
// In SelectAuraC2SPacket.handle():
long now = sender.level().getGameTime();
if (now - state.lastSelectionTime < 5) return; // rate limit
state.lastSelectionTime = now;
```

### 2.2 InstrumentOpenC2SPacket accepts fabricated instrument IDs

**File:** `InstrumentOpenC2SPacket.java:26-35`

The server trusts the client's claimed instrument ID without validation. A client could send any `ResourceLocation`, triggering auto-selection of any mapped default aura. The exploit potential is limited (attacker can only select auras that exist and are enabled) but they could bypass instrument-aura filtering to select auras not intended for their current instrument.

**Recommendation:** Validate the received instrument ID against a list of known instrument IDs, or check that the player's current screen state matches. At minimum, log suspicious IDs at WARN level.

### 2.3 No effect amplifier validation in JSON loader

**File:** `AuraJsonLoader.java:339`

```java
int amplifier = effObj.has("amplifier") ? effObj.get("amplifier").getAsInt() : 0;
```

A custom aura JSON can specify `"amplifier": 255` for Strength, granting Strength 256 — game-breaking. Minecraft's amplifier field is a byte (0-255), so any value in that range is technically "valid" but values above 4 are typically unintended.

**Recommendation:** Clamp the amplifier to a configurable max (default 4) and log a warning if exceeded:

```java
int amplifier = Math.min(effObj.has("amplifier") ? effObj.get("amplifier").getAsInt() : 0, 4);
```

### 2.4 No JSON file size limit

**File:** `AuraJsonLoader.java:279`

`Files.readString(file)` reads the entire file into memory. A maliciously large file placed in the auras directory could cause an OOM. Unlikely in normal use, but worth guarding against.

**Recommendation:** Check `Files.size(file)` before reading and skip files over 64KB with a warning.

---

## 3. High: Performance

### 3.1 Pet entity allowlist parsed every tick interval per musician

**File:** `AuraManager.java:190-192`

```java
Set<ResourceLocation> extraPetTypes = EIServerConfig.PET_ENTITY_ALLOWLIST.get().stream()
        .map(ResourceLocation::new)
        .collect(Collectors.toSet());
```

This creates new `ResourceLocation` objects and a new `HashSet` every 10 ticks per active musician. On a server with 20 musicians and a 5-entry allowlist: 160+ object allocations per second, all immediately garbage-collected.

**Recommendation:** Cache the parsed set as a static field. Invalidate on config change or `/effectiveinstruments reload`. This eliminates all per-tick allocation from this path.

### 3.2 Tick handler iterates all players per level

**File:** `AuraManager.java:130`

```java
for (ServerPlayer player : level.players()) {
```

Iterates every player in every `ServerLevel` every tick interval. On a server with 100 players across 3 dimensions but only 5 active musicians: 300 map lookups per tick interval. The `ConcurrentHashMap.get()` is fast, but this scales linearly with player count.

**Recommendation:** Maintain a `Set<UUID> activeMusicians` that is updated when `instrumentOpen` changes. The tick handler iterates only this set, resolving `ServerPlayer` from the level. This reduces iteration from O(all players) to O(active musicians).

### 3.3 `getEnabledPresets()` creates a new list every call

**File:** `AuraRegistry.java:48-52`

```java
public static List<AuraPreset> getEnabledPresets() {
    return ENABLED_IDS.stream()
            .map(PRESETS::get)
            .filter(Objects::nonNull)
            .toList();
}
```

Called from `InstrumentAuraMapping.getAllowedAuras()` on every instrument screen open. The result is stable between reloads.

**Recommendation:** Pre-compute and cache the enabled presets list in `AuraRegistry.load()`. Return the cached list from `getEnabledPresets()`.

---

## 4. Moderate: Architecture & Design

### 4.1 Static singleton pattern prevents unit testing

**File:** `AuraManager`, `AuraRegistry`, `AuraJsonLoader`, `InstrumentAuraMapping` (all static-only)

All managers use static methods and static state. The existing unit test (`InstrumentAuraMappingJsonTest.java`) had to duplicate the parsing logic rather than testing the actual `InstrumentAuraMapping.parseConfigEntry()` method, because it requires `AuraRegistry` to be loaded with valid presets.

**Assessment:** For a mod of this size, the static pattern is pragmatic and consistent with Forge conventions. No change recommended unless testability becomes a priority. If it does, extract parsing logic into testable pure functions that don't depend on registry state.

### 4.2 Hardcoded default generation is maintenance-heavy

**File:** `AuraJsonLoader.java:31-140` (95 lines), `InstrumentAuraMapping.java:36-96` (60 lines)

15 aura definitions and 23 instrument mappings are hardcoded in Java. Adding a new instrument requires editing Java code, recompiling, and releasing.

**Recommendation:** Bundle the default aura JSONs as resources in the JAR (e.g., under `data/effectiveinstruments/default_auras/`) and copy them to the config directory on first launch. Similarly for instrument mappings. This makes defaults data-driven and reduces the Java source footprint.

### 4.3 No aura ID format validation

**File:** `AuraJsonLoader.java:274-275`

Aura IDs are derived from filenames (strip `.json`). A file named `My Aura!.json` would produce the ID `My Aura!`. This could cause issues in packet strings or log output.

**Recommendation:** Validate that derived IDs match `[a-z0-9_]+` and warn/skip non-conforming files.

### 4.4 AuraPreset stores resolved MobEffect references

**File:** `AuraPreset.java:24-27`

Storing the resolved `MobEffect` reference is efficient at runtime but means presets cannot be serialized or compared across reloads without re-resolving from IDs. This is fine for the current architecture but would need addressing if presets ever need to be synced to clients or serialized.

**Assessment:** No change needed for current scope.

---

## 5. Moderate: UI/UX

### 5.1 No persistent HUD indicator for active aura

**Location:** Not implemented

When a player selects an aura and starts playing, the only visual feedback is colored particles around them in 3rd-person view. In 1st-person (the primary play mode), the player cannot see their own particles. The aura selector is only visible while the instrument screen is open.

**Recommendation:** Add a small, toggleable HUD element (corner icon or status text) displaying the active aura name/icon while playing. Gate behind client config.

### 5.2 Tooltip does not show actual effects

**File:** `AuraButtonWidget.java:78-85`

The tooltip displays `displayName` and `description` but not the actual effects (e.g., "Speed I, Regeneration I"). Users must memorize effects or consult external documentation.

**Recommendation:** Add a third line listing effects with levels. This can be generated from `AuraPreset.effects()` at render time:

```java
// Example output: "Effects: Speed I, Regeneration I"
List<Component> tooltipLines = new ArrayList<>();
tooltipLines.add(preset.displayName());
tooltipLines.add(preset.description());
StringBuilder effectsLine = new StringBuilder("Effects: ");
for (AuraPreset.EffectEntry e : preset.effects()) {
    effectsLine.append(e.effect().getDisplayName().getString());
    if (e.amplifier() > 0) effectsLine.append(" ").append(toRoman(e.amplifier() + 1));
    effectsLine.append(", ");
}
tooltipLines.add(Component.literal(effectsLine.toString().replaceAll(", $", "")));
```

### 5.3 Button overflow on small screens with many auras

**File:** `AuraSelectorWidget.java:45-75`

With 15 auras at default size (20px + 4px gap), total width is 356px — fine on 1080p+. But at scale 2.0 (40px + 8px gap), width becomes 712px, potentially overlapping instrument UI on smaller screens. With custom auras exceeding 15, the problem worsens.

**Recommendation:** Add a maximum width constraint or row wrapping when total button width exceeds a fraction of screen width (e.g., 60%).

### 5.4 Compact mode icon overflow

**File:** `AuraButtonWidget.java:59-65`

Icon rendering uses a fixed `iconSize = 16` regardless of button size. In compact mode (`COMPACT_BUTTON_SIZE = 14`), the 16x16 icon overflows the 14px button by 1px on each side.

**Recommendation:** Scale the icon proportionally:

```java
int iconSize = Math.min(16, width - 2);
```

---

## 6. Balance & Gameplay

### 6.1 Hero of the Village via Smoky Allure (Saxophone) is powerful

**File:** `AuraJsonLoader.java:109-113`

Hero of the Village significantly reduces villager trade prices. As an AoE effect with a 16-block radius, a single saxophone player grants this to all nearby players, enabling discounted trades equivalent to winning a raid — but repeatable and effortless.

**Assessment:** This is an intentional thematic choice (documented in INSTRUMENT_AURAS.md: "Charismatic jazz melodies charm villagers into offering better trades"). It is bounded by requiring active note-playing. However, server admins may want to disable it.

**Recommendation:** Add a note in the README about this being a powerful economic effect. Consider documenting that admins can disable it by editing `smoky_allure.json` (set `"enabled": false`) or changing the effect.

### 6.2 Absorption effect provides persistent extra hearts

**File:** `AuraManager.java:230-244`

Absorption re-applies every 10 ticks (aura tick interval). In Minecraft, re-applying Absorption resets absorption health. This means Bloom Veil (Absorption I = 4 hearts), Bulwark Fanfare (Absorption II = 8 hearts), and Heartstring Aria (Absorption I = 4 hearts) provide effectively permanent absorption while the aura is active. Bulwark Fanfare's Absorption II is particularly powerful for sustained combat — 8 extra hearts that regenerate every 0.5 seconds.

**Assessment:** This is the expected Minecraft behavior for refreshing absorption effects. The balance is acceptable since it requires active playing. No change recommended, but document this interaction for players.

### 6.3 Five-second note window is very generous

**File:** `EIServerConfig.java:29-30` (default 100 ticks)

A player can stop playing for a full 5 seconds and the aura remains active. Even very casual/slow playing (one note every 4 seconds) maintains the aura indefinitely.

**Assessment:** This is clearly an intentional design decision, configurable from 20-600 ticks. The generous default supports casual and accessible play. No change needed.

### 6.4 Luck effect (Sunkissed Serenade) has limited utility

**File:** `AuraJsonLoader.java:71-75`

The Luck effect only affects loot table draws. It has no effect on mob drops (those use the Looting enchantment), only on chest loot and fishing. This makes the Ukulele's aura situational compared to others.

**Assessment:** Fair balance choice. The situational nature offsets the uniqueness of the effect. Consider documenting what Luck actually affects so players don't overestimate it.

---

## 7. Content Consistency

### 7.1 Test file references obsolete aura names

**File:** `InstrumentAuraMappingJsonTest.java` (throughout)

Tests use `"soothing_hymn"`, `"guardian_chorus"`, and `"invigorating_march"` — names from v1.0.0 that no longer correspond to any aura in the current registry. Tests still pass because they validate parsing logic in isolation (without registry validation), but the stale names are confusing for readers.

**Recommendation:** Update test data to use current aura names (`"zephyrs_blessing"`, `"echoes_of_antiquity"`, `"warcry_cadence"`, etc.).

### 7.2 Uncommitted texture files and documentation changes

**Location:** Git working tree

The working tree has significant uncommitted changes:
- **4 old texture pairs deleted** (guardian_chorus, invigorating_march, luminous_nocturne, soothing_hymn) — from v1.0.0
- **30 new texture files untracked** — the 15 aura icon pairs for v1.2.0
- **Modified:** `CHANGELOG.md`, `CURSEFORGE_DESCRIPTION.md`, `README.md`, `AuraJsonLoader.java`

The v1.2.0 commit (`b1dfaaf`) was made **without** these texture files. Anyone building from the current commit will have no icon textures for any of the 15 auras — the UI will fall back to letter rendering for all buttons.

**Recommendation:** Stage and commit all changes immediately. The missing textures are a release blocker.

### 7.3 tools/ directory is untracked

**Location:** `tools/generate_aura_icons.py`

The icon generation script is in `tools/` which appears to be untracked (shown as `??` in git status). This script is the source-of-truth for generating the 30 aura icon PNGs. If it's lost, the icons would need to be regenerated from scratch.

**Recommendation:** Commit the `tools/` directory.

---

## 8. Code Quality

### 8.1 ConcurrentHashMap is misleading

**File:** `AuraManager.java:27`

```java
private static final Map<UUID, PlayerAuraState> PLAYER_STATES = new ConcurrentHashMap<>();
```

`ConcurrentHashMap` suggests thread-safety was a concern. However, `PlayerAuraState` fields are mutable, unsynchronized, and all access occurs on the main server thread (tick handlers, packet handlers via `enqueueWork()`). The `ConcurrentHashMap` provides no real benefit and signals false safety guarantees.

**Recommendation:** Either replace with `HashMap` (since all access is on the main thread) and add a comment noting this, or synchronize `PlayerAuraState` field access if concurrent access is actually intended.

### 8.2 `onInstrumentClose` bypasses `clearAuraSelection()` logging

**File:** `AuraManager.java:101-109`

The method directly sets `state.selectedAura = null` instead of calling `clearAuraSelection(player)`, which would log the deselection at DEBUG level. This means instrument-close deselections are not logged, while explicit deselections (via packet) are.

**Recommendation:** Call `clearAuraSelection(player)` instead of directly nulling the field.

### 8.3 `PlayerAuraState` fields have package-private access

**File:** `AuraManager.java:29-37`

`PlayerAuraState` is a `public static class` with package-private (default) field access for `selectedAura`, `currentInstrumentId`, `lastNoteGameTime`, and `instrumentOpen`. These are mutated directly from `AuraManager` static methods. This is functional but blurs the encapsulation boundary.

**Assessment:** For a nested class in the same file, this is an acceptable Forge-style pattern. No change needed unless the class is extracted.

### 8.4 Unused `@SuppressWarnings("deprecation")` context

**File:** `AuraNoteParticleOptions.java:22`

The `@SuppressWarnings("deprecation")` on the `DESERIALIZER` field suppresses the deprecation warning for `ParticleOptions.Deserializer`. This is correct for Forge 47.x — the deprecated API is still the required approach.

**Assessment:** No action needed. Remove the suppression only when migrating to NeoForge/newer APIs.

---

## 9. Documentation

### 9.1 README does not link to INSTRUMENT_AURAS.md

**File:** `README.md`

The detailed design reference (`INSTRUMENT_AURAS.md`) is not linked from the README. Developers and power users who would benefit from understanding aura design choices may not discover it.

**Recommendation:** Add a link under "Technical Details" or as a "Design Reference" section.

### 9.2 No in-game discovery mechanism

**Location:** Not implemented

New players have no way to discover the aura system other than noticing the small buttons in the top-right corner of the instrument screen. There is no tutorial tooltip, first-use prompt, advancement, or in-game help.

**Recommendation:** Consider adding a brief chat message on first instrument open (per-player, stored in player capabilities or a data tag): *"Tip: Musical auras grant buffs while you play! Select an aura from the buttons in the top-right corner."* Gate behind a `showFirstUseHint` client config option.

### 9.3 Only English localization

**File:** `src/main/resources/assets/effectiveinstruments/lang/en_us.json` (11 keys)

Only English strings exist. The aura JSON `displayName`/`description` fields support MC Component format (including `{"translate":"..."}`) which is good forward-looking design, but the default aura names are hardcoded English strings.

**Assessment:** English-only is fine for the current scope. If i18n is desired in the future, convert default aura names to translation keys.

### 9.4 Config path not explicit in README

**File:** `README.md`

The README references configuration options but doesn't explicitly state the config file path (`config/effective_instruments/`). Users unfamiliar with Forge config conventions may look in the wrong location.

**Recommendation:** Add the full path in the Configuration section.

---

## 10. Build & CI

### 10.1 Dependencies are local JARs

**File:** `build.gradle:53-54`

```groovy
repositories {
    flatDir { dir 'Dependencies' }
}
```

The project depends on local JAR files in a `Dependencies/` directory that is `.gitignore`d. Anyone cloning the repo cannot build without first obtaining these JARs. The CI workflow would fail unless the JARs are available through another mechanism.

**Recommendation:** Add instructions to the README for obtaining Genshin Instruments and Even More Instruments JARs. If the mods have public Maven repositories, use those instead. If not, document the exact download URLs and expected filenames.

### 10.2 CI runs only `./gradlew build`

**File:** `.github/workflows/build.yml:23`

The CI runs `./gradlew build` which includes compilation, tests, and JAR packaging. This is functional but there is no:
- Test result reporting (JUnit XML output)
- Code coverage tracking
- Artifact versioning (version is always `1.2.0` from `gradle.properties`)
- Release automation (manual process)

**Assessment:** For a mod of this size, the current CI is adequate. Consider adding a release workflow (tag-triggered with version extraction) when the mod matures.

### 10.3 No Gradle wrapper JAR committed

**File:** `.gitignore:4`

The `.gitignore` excludes `gradle/wrapper/gradle-wrapper.jar`. Without the wrapper JAR, `./gradlew build` cannot bootstrap Gradle. Users (and CI) would need Gradle 8.9 installed globally, or the wrapper JAR must be committed separately.

**Recommendation:** Either commit the wrapper JAR (standard practice for most projects) or add CI steps to install Gradle explicitly. The current CI uses `gradle/actions/setup-gradle@v4` which handles this, but local developers would be affected.

---

## 11. Compatibility

### 11.1 DistExecutor.unsafeRunWhenOn is deprecated

**File:** `SyncAuraSelectionS2CPacket.java:29`

```java
DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
        AuraOverlayInjector.onServerSyncAura(msg.auraId));
```

`DistExecutor.unsafeRunWhenOn` is deprecated in newer Forge versions. For Forge 47.x (1.20.1), it still works and is the standard approach.

**Assessment:** No action needed for the current target. Will need updating for any future Forge/NeoForge port.

### 11.2 EMI compileOnly dependency is correctly handled

**File:** `build.gradle:64`

Even More Instruments is `compileOnly`, meaning it won't be in the runtime classpath. The client code checks `screen instanceof InstrumentScreen` which catches EMI screens since they extend GI's `InstrumentScreen`. This is the correct pattern for optional soft dependencies.

**Assessment:** No issues. Clean implementation.

### 11.3 Protocol version handling is standard

**File:** `EIPacketHandler.java:14-19`

Protocol version `"3"` with string comparison is standard Forge networking practice. Clients and servers with mismatched mod versions will correctly fail to connect with a version mismatch error.

**Assessment:** No issues.

---

## 12. Prioritized Action Items

### Immediate (before next release)

| # | Finding | Section | Effort |
|---|---------|---------|--------|
| 1 | **Commit uncommitted textures, docs, and tools/** | 7.2, 7.3 | 5 min |
| 2 | **Fix player logout effect cleanup** | 1.1 | 10 min |
| 3 | **Add effect amplifier validation** (clamp to 0-4) | 2.3 | 5 min |
| 4 | **Fix compact mode icon overflow** | 5.4 | 2 min |

### Short-term (next patch)

| # | Finding | Section | Effort |
|---|---------|---------|--------|
| 5 | **Cache pet entity allowlist** | 3.1 | 15 min |
| 6 | **Add packet rate limiting** | 2.1 | 20 min |
| 7 | **Add effect listing to tooltips** | 5.2 | 20 min |
| 8 | **Update test data to current aura names** | 7.1 | 10 min |
| 9 | **Document dimension change cleanup limitation** | 1.2 | 5 min |
| 10 | **Link INSTRUMENT_AURAS.md from README** | 9.1 | 2 min |
| 11 | **Document Smoky Allure balance implications** | 6.1 | 5 min |

### Medium-term (future version)

| # | Finding | Section | Effort |
|---|---------|---------|--------|
| 12 | **Add persistent HUD indicator for active aura** | 5.1 | 2-3 hr |
| 13 | **Optimize tick handler to iterate only active musicians** | 3.2 | 30 min |
| 14 | **Move default auras to bundled JAR resources** | 4.2 | 1 hr |
| 15 | **Add aura ID format validation** | 4.3 | 15 min |
| 16 | **Replace ConcurrentHashMap or document rationale** | 8.1 | 5 min |
| 17 | **Add button row wrapping for many auras** | 5.3 | 1 hr |
| 18 | **Add JSON file size limit** | 2.4 | 5 min |
| 19 | **Validate fabricated instrument IDs** | 2.2 | 30 min |

### Nice-to-have

| # | Finding | Section | Effort |
|---|---------|---------|--------|
| 20 | **In-game first-use hint** | 9.2 | 1 hr |
| 21 | **Localization support** | 9.3 | 2+ hr |
| 22 | **CI release automation** | 10.2 | 1 hr |
| 23 | **Cache enabled presets list** | 3.3 | 5 min |
| 24 | **Effect cleanup duration heuristic** | 1.3 | 30 min |

---

## Appendix: Files Reviewed

| File | Lines | Role |
|------|-------|------|
| `EffectiveInstrumentsMod.java` | 73 | Mod entry point, initialization |
| `aura/AuraManager.java` | 316 | Core: player state, effect application, particles |
| `aura/AuraPreset.java` | 46 | Record: aura data model |
| `aura/AuraRegistry.java` | 56 | Singleton: preset storage and queries |
| `aura/AuraJsonLoader.java` | 389 | JSON parsing, default generation |
| `aura/InstrumentAuraMapping.java` | 319 | Instrument-to-aura mapping |
| `event/InstrumentStateHandler.java` | 53 | Forge event subscribers (open/close/tick/logout) |
| `event/NoteActivityHandler.java` | 37 | Note played event subscriber |
| `network/EIPacketHandler.java` | 51 | Packet registration |
| `network/packet/SelectAuraC2SPacket.java` | 70 | C2S: aura selection |
| `network/packet/InstrumentOpenC2SPacket.java` | 36 | C2S: instrument open with ID |
| `network/packet/SyncAuraSelectionS2CPacket.java` | 34 | S2C: sync auto-selected aura |
| `config/EIServerConfig.java` | 62 | Server config spec |
| `config/EIClientConfig.java` | 59 | Client config spec |
| `command/EICommands.java` | 85 | /effectiveinstruments commands |
| `client/event/AuraOverlayInjector.java` | 139 | Screen injection, aura selection |
| `client/event/EIClientSetup.java` | 24 | Particle provider registration |
| `client/widget/AuraSelectorWidget.java` | 129 | Aura button container |
| `client/widget/AuraButtonWidget.java` | 122 | Individual aura button |
| `client/particle/AuraNoteParticle.java` | 74 | Particle renderer |
| `client/particle/AuraNoteParticleProvider.java` | 42 | Particle factory |
| `particle/EIParticleTypes.java` | 21 | Particle type registration |
| `particle/AuraNoteParticleOptions.java` | 59 | Particle data (RGB color) |
| `InstrumentAuraMappingJsonTest.java` | 240 | Unit tests for JSON parsing |
| `build.gradle` | 115 | Build configuration |
| `gradle.properties` | 28 | Build properties |
| `mods.toml` | 39 | Mod metadata |
| `en_us.json` | 12 | English localization |
| `aura_note.json` | 1 | Particle definition |
| `build.yml` | 29 | CI workflow |
| `.gitignore` | 42 | Git ignore rules |
| `README.md` | 150 | Project documentation |
| `CHANGELOG.md` | 57 | Version history |
| `CURSEFORGE_DESCRIPTION.md` | 223 | CurseForge listing |
| `INSTRUMENT_AURAS.md` | 516 | Design reference |
| `generate_aura_icons.py` | 463 | Icon generation tool |
| **30 PNG textures** | — | GUI aura icons |
| **8 PNG textures** | — | Particle note sprites |

**Total: ~1,300 LOC main source, 240 LOC test, ~2,000 LOC documentation, 463 LOC tooling**
