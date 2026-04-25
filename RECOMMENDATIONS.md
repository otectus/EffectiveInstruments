# Effective Instruments — Bug-Fix, Optimization & Improvement Recommendations

**Reviewed against:** v1.4.8 (commit at HEAD on 2026-04-25)
**Scope:** Bug fixes & correctness, code quality & architecture, new features
**Prioritization:** Severity (Critical / Major / Minor / Polish) + effort/impact tags

Every entry cites concrete files and line numbers. Effort tags use **S** (under an hour), **M** (under a day), **L** (multi-day). Impact tags are user-facing severity, not author-side mental load.

---

## Executive Summary

The codebase is in good shape after the 1.4.x hotfix series — the aura tick loop, durability NBT layer, and offensive-pairing synthesis are all defensive and well-commented. The bulk of the issues below fall into four buckets:

1. **A handful of latent correctness bugs** centered on lifecycle ordering: SERVER-config reads at the wrong time, durability-default fallback that doesn't actually fall back, and a couple of unprotected packet handlers.
2. **Documentation drift** — comments and lang strings reference 1.3.0/1.4.0 limitations that 1.4.3+ already fixed.
3. **Asymmetric defensive coding** — atomic writes, throttles, and try/catch guards exist on some files/handlers but not their siblings.
4. **Architecture cleanup** opportunities: deprecated config keys nobody reads, dead reflection plumbing, two icon generators, etc.

If you ship 1.4.9 with **§1 Critical** + **§2 Major** + the lang/comment fixes from §3, you'll resolve every functional regression risk currently in the tree. Everything else is incremental quality work.

---

## §1 — Critical (Functional bugs likely to bite real users)

### 1.1 Durability default-max fallback never fires

**File:** `src/main/java/com/crims/effectiveinstruments/durability/InstrumentDurability.java:54-64`
**Companion file:** `src/main/java/com/crims/effectiveinstruments/config/InstrumentDurabilityConfig.java:24-32` (doc claim)

`InstrumentDurabilityConfig`'s class doc reads:

> "missing entries fall back to the `DURABILITY_DEFAULT_MAX` server config and no repair material."

But the code path is:

```java
private static InstrumentDurabilityConfig.Entry resolveEntry(ItemStack stack) {
    if (stack.isEmpty()) return null;
    InstrumentDurabilityConfig.Entry cached = ENTRY_CACHE.get(stack.getItem());
    if (cached != null) return cached == NO_ENTRY ? null : cached;
    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
    InstrumentDurabilityConfig.Entry entry = id == null
            ? null
            : InstrumentDurabilityConfig.get(id);   // returns null if not in JSON
    ENTRY_CACHE.put(stack.getItem(), entry == null ? NO_ENTRY : entry);
    return entry;
}
```

If `InstrumentDurabilityConfig.get(id)` returns `null` (item not in `instrument_durability.json`), `resolveEntry` returns `null`, `getMax` returns 0, and `isTracked` returns `false`. Net effect: any instrument from a third-party mod that isn't pre-listed in the durability JSON has **no durability tracking at all** — exactly the opposite of what the doc promises and what the `EIServerConfig.DURABILITY_DEFAULT_MAX` config knob implies.

**Fix:** if the lookup misses, synthesize a default `Entry`:

```java
ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
InstrumentDurabilityConfig.Entry entry = id == null ? null : InstrumentDurabilityConfig.get(id);
if (entry == null && id != null && isLikelyInstrument(id)) {
    int defaultMax = EIServerConfig.DURABILITY_DEFAULT_MAX.get();
    entry = new InstrumentDurabilityConfig.Entry(defaultMax, null, Math.max(1, defaultMax / 5));
}
```

You'll want a cheap `isLikelyInstrument` predicate (namespace allowlist of `genshinstrument`, `evenmoreinstruments`, `immersive_melodies` matches the existing `InstrumentDurabilityBarDecorator.INSTRUMENT_MOD_IDS` set — extract it to a shared helper). Without that gate every item in the game becomes "durability-tracked" with an invisible NBT and a fallback bar.

**Effort:** S — Impact: High (silent feature-gap for any instrument not in the shipped JSON; affects modpack authors most).

---

### 1.2 `AuraRegistry.load()` runs at common-setup, before SERVER config is loaded

**File:** `src/main/java/com/crims/effectiveinstruments/EffectiveInstrumentsMod.java:50-56`

```java
private void commonSetup(final FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        EIPacketHandler.register();
        AuraRegistry.load();                  // ← reads EIServerConfig values
        ImmersiveMelodiesCompat.init();
    });
}
```

`AuraRegistry.load()` calls `InstrumentDurabilityConfig.load()` which, on entries missing `maxDurability`, reads `EIServerConfig.DURABILITY_DEFAULT_MAX.get()` (line 199 of `InstrumentDurabilityConfig.java`). Forge `Type.SERVER` configs are not loaded until `ServerAboutToStartEvent` (or the world-loading pipeline on the integrated server). At common-setup, `IntValue.get()` is in the not-yet-loaded state — depending on Forge version it returns the spec default or throws `IllegalStateException`. Either way, the in-tree contract is brittle.

Equally important, `AuraApplicator.invalidatePetAllowlistCache()` at the bottom of `AuraRegistry.load()` reads `EIServerConfig.PET_ENTITY_ALLOWLIST.get()` — the cached set is empty until reload, so the very first world session has no pet allowlist.

**Fix:** Split the load into a "preset/JSON only" pass at common-setup and a "config-dependent" refresh on `ServerAboutToStartEvent`:

```java
@SubscribeEvent
public void onServerAboutToStart(ServerAboutToStartEvent event) {
    AuraRegistry.refreshConfigDerived();   // pet allowlist, durability defaults, etc.
}
```

…and gate `InstrumentDurabilityConfig.load()` to skip the `DURABILITY_DEFAULT_MAX` fallback at common-setup time, deferring it to the server-start refresh.

**Effort:** M — Impact: High (manifests as "first world session has no aura particles" or "pet allowlist empty for the first instrument"; quietly self-corrects on `/effectiveinstruments reload`, masking the bug).

---

### 1.3 SERVER-config reads on the client-render path can NPE pre-join

**File:** `src/main/java/com/crims/effectiveinstruments/client/event/InstrumentDurabilityBarDecorator.java:67-95`

```java
private static boolean renderBar(GuiGraphics graphics, Font font, ItemStack stack, int xOffset, int yOffset) {
    if (stack.isEmpty()) return false;
    if (!EIServerConfig.DURABILITY_ENABLED.get()) return false;   // ← unprotected
    ...
}
```

`DURABILITY_ENABLED` is a `Type.SERVER` config. On a connected client it's pushed by the server during handshake; on the title screen, world list with item previews, REI/JEI panels rendered before joining, etc. it's not loaded. The sibling `DurabilityTooltipHandler.onTooltip` already wraps this exact call:

```java
try {
    if (!EIServerConfig.DURABILITY_ENABLED.get()) return;
} catch (IllegalStateException ignored) {
    return;
}
```

The decorator should do the same. (Same reasoning at line 71, line 78 only depends on cached entry.)

**Fix:** Apply the same try/catch pattern as `DurabilityTooltipHandler.onTooltip:27-31`. Better: extract a `EIServerConfig.isDurabilityEnabledSafe()` static helper that returns `false` when the config isn't loaded.

**Effort:** S — Impact: Medium (intermittent crash on item-preview screens with mods that render instruments outside in-game).

---

### 1.4 `InstrumentOpenC2SPacket.handleMobile` has no rate-limit; `SelectAuraC2SPacket.handleMobile` likewise

**Files:**
- `src/main/java/com/crims/effectiveinstruments/network/packet/InstrumentOpenC2SPacket.java:93-99`
- `src/main/java/com/crims/effectiveinstruments/network/packet/SelectAuraC2SPacket.java:148-184`

The stationary path (`InstrumentOpenC2SPacket.handle:75-78` and `SelectAuraC2SPacket.handleStationary:77-82`) has a 5-tick cooldown. The mobile paths do not. `SelectAuraC2SPacket.handleMobile` calls `MobilePlayerSelection.setSelection` which calls `setDirty()` on the SavedData — a malicious client can flood selections and force the server to mark the SavedData dirty on every packet, increasing autosave I/O.

`InstrumentOpenC2SPacket.handleMobile` flips `screenOpenInstrumentId` on `ImmersiveMelodiesAuraHandler` — at high frequency this thrashes the activation gate.

**Fix:** add the same throttle. For mobile select, keep a `Map<UUID, Long>` of last-mobile-selection-tick on the packet handler (or move the rate-limit state into `MobilePlayerSelection` itself). For mobile open/close, reuse `AuraManager.PlayerAuraState.lastOpenPacketTick` or add a separate per-player throttle on `ImmersiveMelodiesAuraHandler`.

**Effort:** S — Impact: Medium (server-DoS surface area on multiplayer; not exploitable today on single-player).

---

### 1.5 Stale lang string misdirects players

**File:** `src/main/resources/assets/effectiveinstruments/lang/en_us.json` (last entry)

```json
"tooltip.effectiveinstruments.aura_hint": "Right-click to open the aura selector (top-right)"
```

Right-click opens the **instrument**, not the selector. The selector appears in the top-right *as a side effect* of the screen opening. The string is added to every tracked-instrument tooltip via `DurabilityTooltipHandler:57-58`, so every new player reads it and gets confused.

**Fix:**

```json
"tooltip.effectiveinstruments.aura_hint": "Open the instrument to choose an aura (top-right of the screen)"
```

**Effort:** S — Impact: Medium (UX papercut, hits every new player).

---

## §2 — Major (Correctness gaps without immediate user impact, but worth fixing in 1.4.9)

### 2.1 Stale "Free-play not supported" comments and lang strings

The mod gained free-play support in 1.4.3 (per CLAUDE.md), but several locations still claim it's a known limitation:

- `src/main/java/com/crims/effectiveinstruments/compat/immersivemelodies/ImmersiveMelodiesAuraHandler.java:30-33` — class doc "Known limitation (1.3.0)..."
- `src/main/java/com/crims/effectiveinstruments/compat/immersivemelodies/ImmersiveMelodiesCompat.java:29-31` — class doc "Known limitation for 1.3.0..."
- `src/main/resources/assets/effectiveinstruments/lang/en_us.json` — `tooltip.effectiveinstruments.compat.immersive_melodies.limitations`: "Mobile buffs fire for autoplay / selected-melody playback only. Free-play keyboard/MIDI mode is not supported in 1.3.0."

Update or delete each. The lang string in particular is user-facing.

**Effort:** S — Impact: Medium (confuses users who think their setup is broken).

---

### 2.2 Stale comment in `EntityCategory.classify` about "extra pet types"

**File:** `src/main/java/com/crims/effectiveinstruments/aura/EntityCategory.java:66-86`

The comment block at lines 66-69 says:

> "Extra pet types (configured allowlist) — owner tracking varies per mod so we treat them as musician-owned only when the existing tame checks below don't already catch them."

But the code at lines 83-86 categorizes extra-pet types as `PASSIVE_MOB`, never `OWN_PET`. The actual behavior is sensible (owner identity for foreign mods is unknowable), but the comment promises something the code doesn't do.

**Fix:** rewrite the comment to match reality:

```java
// Extra pet types (configured allowlist) — for mobs whose mod doesn't
// expose a TamableAnimal-style ownership API we can't tell which player
// owns them. Bucket as PASSIVE_MOB (admin-domesticated) rather than
// OWN_PET, so positive auras still cover them when the positive
// allowlist enables PASSIVE_MOB.
```

**Effort:** S — Impact: Low (developer-confusion only, surface bug in code review).

---

### 2.3 Deprecated config keys are declared, never read, never warned

**File:** `src/main/java/com/crims/effectiveinstruments/config/EIServerConfig.java`

The following config values exist in the spec, are documented as DEPRECATED, and are never accessed at runtime:

| Key | Line | Comment claims |
| --- | --- | --- |
| `ALLOW_SELF_BUFF` | 116-118 | "ignored at runtime" |
| `INCLUDE_OTHER_PLAYERS` | 119-121 | "Still read as a migration default on first boot" — false |
| `INCLUDE_TAMED_PETS` | 122-124 | "ignored at runtime" |
| `MAX_TARGETS_PER_TICK` | 125-129 | "Still honored as a fallback cap when the positive block is at its default" — false |
| `OFFENSIVE_ALLOW_SELF` | 222-224 | "ignored at runtime" |
| `OFFENSIVE_INCLUDE_TAMED_PETS` | 228-230 | "Read for a one-shot migration warning" — false |
| `MOBILE_ALLOW_SELF_BUFF` | 192-194 | (Comment in `ImmersiveMelodiesAuraHandler:271-274` says deprecated) |
| `MOBILE_INCLUDE_OTHER_PLAYERS` | 195-197 | (Same) |
| `MOBILE_INCLUDE_TAMED_PETS` | 198-200 | (Same) |

There's no startup code that reads these values for a deprecation warning either. Two paths forward:

**Option A — implement what the comments promise.** Add a one-shot warn pass on `ServerAboutToStartEvent`:

```java
@SubscribeEvent
public void onServerAboutToStart(ServerAboutToStartEvent e) {
    warnIfDeprecated("targeting.allowSelfBuff", ALLOW_SELF_BUFF.get(), true);
    warnIfDeprecated("targeting.includeTamedPets", INCLUDE_TAMED_PETS.get(), true);
    warnIfDeprecated("offensiveTargeting.allowSelf", OFFENSIVE_ALLOW_SELF.get(), false);
    // ...
}
private static void warnIfDeprecated(String key, boolean actual, boolean specDefault) {
    if (actual != specDefault) {
        LOGGER.warn("Config key '{}' is deprecated and ignored at runtime; please remove from server.toml.", key);
    }
}
```

**Option B — delete them outright** (1.5.0 milestone). They've been dead since 1.4.1; the migration window for users who hand-edited their `server.toml` to flip these is over. Removing them tightens the spec from 38 keys to 30, easier to scan.

Recommend **Option A** for 1.4.9 (keeps existing TOMLs valid), **Option B** for 1.5.0.

**Effort:** S (Option A) / M (Option B with migration notes) — Impact: Low (server admins) but Medium (codebase clarity).

---

### 2.4 Anvil repair doesn't honor work-penalty NBT

**File:** `src/main/java/com/crims/effectiveinstruments/event/InstrumentAnvilHandler.java:36-98`

Vanilla anvils track per-stack `RepairCost` NBT that doubles each successful anvil use, eventually making further repairs cost prohibitive. `setOutput(...)` + `setCost(2)` sidesteps that entirely — a player can endlessly combine two damaged copies for 2 levels. Same for material-based repair: `setCost(Math.max(1, materialsNeeded * 2))` is a flat formula that doesn't escalate.

**Fix:** mirror `AnvilMenu.createResult` semantics:

```java
ItemStack output = left.copy();
int prevRepairCost = left.getBaseRepairCost();
int newCost = AnvilMenu.calculateIncreasedRepairCost(prevRepairCost);
output.setRepairCost(newCost);
// ...
event.setCost(newCost + materialsNeeded * 2);   // base growth + material penalty
```

(`AnvilMenu.calculateIncreasedRepairCost` is `prev * 2 + 1`.)

**Effort:** S — Impact: Medium (balance issue; instruments are too cheap to keep repaired).

---

### 2.5 Atomic writes are inconsistent across writers

`InstrumentAuraMapping` has a `writeAtomically` helper (line 440) and uses it in two places (`ensureOffensiveAllowedLists`, `ensureUniqueAssignment`, and `tryRewriteMappingFile`). Every other writer in the mod uses raw `Files.writeString`:

| File | Writer | Atomic? |
| --- | --- | --- |
| `InstrumentAuraMapping.java` | `ensureFirstRunDefaults:214` | ❌ |
| `InstrumentAuraMapping.java` | `ensureOffensiveAllowedLists:320` | ✅ |
| `InstrumentAuraMapping.java` | `ensureUniqueAssignment:413` | ✅ |
| `InstrumentAuraMapping.java` | `tryRewriteMappingFile:829` | ✅ |
| `MobileInstrumentAuraMapping.java` | `ensureFirstRunDefaults:116` | ❌ |
| `MobileInstrumentAuraMapping.java` | `ensureOffensiveAllowedLists:197` | ❌ |
| `MobileInstrumentAuraMapping.java` | `tryRewriteMappingFile:333` | ❌ |
| `InstrumentDurabilityConfig.java` | `ensureDefaults:137` | ❌ |
| `AuraJsonLoader.java` | `writeDefaultJson:733` | ❌ |
| `AuraJsonLoader.java` | `writeOffensiveStationaryDefault:596` | ❌ |
| `AuraJsonLoader.java` | `writeOffensiveMobileDefault:618` | ❌ |
| `AuraJsonLoader.java` | `writeMobileDefault:701` | ❌ |
| `AuraJsonLoader.java` | `writeReadme:796` | ❌ |
| `InstrumentAuraMapping.java` | `writeReadme:537` | ❌ |

A power-loss mid-write to any of these files corrupts the user's config. The risk is low for marker-gated first-run paths (they're written once and never overwritten), but `tryRewriteMappingFile` on the mobile mapping IS in the steady-state self-healing rewrite path and runs every load — it should be atomic.

**Fix:** lift `writeAtomically` from `InstrumentAuraMapping` into a shared `ConfigIO` utility class and route every config-writer through it. Preset/README writes can remain non-atomic if you want to minimize churn — those files don't change after first install.

**Effort:** M — Impact: Low (rare scenario, but data-loss when it bites is annoying).

---

### 2.6 Mobile-tier server tick scans every player every pulse

**File:** `src/main/java/com/crims/effectiveinstruments/compat/immersivemelodies/ImmersiveMelodiesAuraHandler.java:59-70`

```java
public static void onServerTick(ServerLevel level) {
    if (!ImmersiveMelodiesCompat.isAvailable()) return;
    if (!EIServerConfig.MOBILE_TIER_ENABLED.get()) return;
    // ...
    for (ServerPlayer player : level.players()) {
        tickPlayer(level, player, gameTime);
    }
}
```

Compare to the stationary tier (`AuraManager.onServerTick:201`), which iterates only `activeMusicians` (a set you maintain in `onNotePlayed`/`onInstrumentOpen`). On a 100-player public server, the mobile tier does 100 hash-map probes per second per dimension even when nobody's playing an IM instrument.

**Fix:** maintain a parallel `activeMobileMusicians` set on `ImmersiveMelodiesAuraHandler`, populated from:
- `onScreenOpened` (already a lifecycle event)
- A "first NBT-playing tick" check (you're already doing the lookup once per pulse — only add the player on the first positive)

…and tear down on `onScreenClosed`, `onPlayerLogout`, and a "no held + no screenOpen" idle pulse.

**Effort:** M — Impact: Medium (TPS budget on large servers; not a problem for solo play).

---

### 2.7 `affectedTargets` map can accumulate stale dimension-changed entries

**Files:**
- `src/main/java/com/crims/effectiveinstruments/aura/AuraManager.java:265-278`
- `src/main/java/com/crims/effectiveinstruments/aura/AuraApplicator.java:81-115`

The doc comment at `clearPreviousAuraEffects` (line 269) acknowledges this:

> "When called after a dimension change, musician.serverLevel() returns the NEW dimension. Entity lookups for targets in the OLD dimension will return null, so those effects are not actively cleaned up."

But there's a separate problem: even within the same dimension, dead entities (mob killed during aura) leave their entity-id in `state.affectedTargets`. Over a long session, the map grows unbounded with dead-mob ids. The map is cleared on aura switch via `AuraApplicator.clear`, but never if the player just keeps playing the same aura.

**Fix:** during the periodic tick (`onServerTick`), prune entries where `level.getEntity(id) == null`:

```java
state.affectedTargets.keySet().removeIf(id -> level.getEntity(id) == null);
```

…called once per minute or so to amortize the cost.

**Effort:** S — Impact: Low (memory grows slowly; unlikely to cause issues in practice but unbounded is unbounded).

---

### 2.8 `radiusOverride` has no upper bound

**File:** `src/main/java/com/crims/effectiveinstruments/aura/AuraJsonLoader.java:917`

```java
int radius = root.has("radius") ? root.get("radius").getAsInt() : -1;
```

A user-edited (or maliciously-shipped pack) JSON can set `"radius": 10000`. `AuraApplicator.gatherTargets` then inflates the player's bounding box by 10000 blocks, calling `level.getEntities(source, AABB)` over a 20k-cube area. On a populated server, that allocates and walks every loaded entity in range — instant TPS death.

**Fix:** clamp at parse time to the same range as the global config knob (1-64):

```java
int rawRadius = root.has("radius") ? root.get("radius").getAsInt() : -1;
int radius = rawRadius < 0 ? -1 : Math.max(1, Math.min(rawRadius, 64));
if (rawRadius != radius && rawRadius != -1) {
    EffectiveInstrumentsMod.LOGGER.warn(
        "Aura '{}' radius {} clamped to {} (max 64)", id, rawRadius, radius);
}
```

(Same parse-time clamp for `durationTicks` — currently unbounded; an aura can specify 24000 ticks (20 minutes) and effects will linger far past the 13-second cleanup window assumed by `AuraApplicator.clear`.)

**Effort:** S — Impact: High in failure mode, but failure mode requires hostile JSON.

---

## §3 — Minor (Polish, code clarity, low-risk simplifications)

### 3.1 Two icon generators present, only one is canonical

**Files:**
- `tools/gen_aura_icons.py` (canonical per CLAUDE.md 1.4.2 notes)
- `tools/generate_aura_icons.py` (legacy; per CLAUDE.md should have been deleted)

`generate_aura_icons.py` (462 lines) is dead. Either delete it or add a runtime check that prints "use gen_aura_icons.py instead" and exits.

**Effort:** S — Impact: Low (developer-confusion only).

---

### 3.2 `instrumentOpen` flag is now mostly dead state

**File:** `src/main/java/com/crims/effectiveinstruments/aura/AuraManager.java:35, 67-83, 132-173`

Per the 1.4.7 hotfix notes, `isActive` no longer requires `instrumentOpen=true` — the recent-notes window is the authoritative signal. The flag still gets set in `onInstrumentOpen`, cleared in `onInstrumentClose`, and reported in `/effectiveinstruments status` and `/effectiveinstruments diagnose`. Three reads remain:

1. `onInstrumentClose:166` — gates whether to call `onAuraSwitch`.
2. `EICommands.runDiagnose:152` — diagnostic display.
3. `EICommands.showStatus:288` — status display ("Instrument Open: yes/no").

The clean-up gate at `onInstrumentClose:166` is now problematic: if a player triggered the aura via `onNotePlayed` (which adds them to `activeMusicians` but doesn't flip `instrumentOpen`), then closes the instrument via `onInstrumentClose`, the gate is false and `onAuraSwitch` doesn't fire — applied effects stay on tracked targets until they expire naturally. The display strings make it look like the player still has an active instrument when they don't.

**Fix:** drop the `instrumentOpen && ...` gate in `onInstrumentClose` and unconditionally call `onAuraSwitch` + `clearAuraSelection`. Repurpose `instrumentOpen` to mean "the GI screen is currently rendered on the client" (its original intent) and stop using it as a clean-up gate. Diagnostic strings become honest again.

**Effort:** S — Impact: Medium (effects linger across screen-close in a corner case).

---

### 3.3 `offensiveOwners` audit always recomputes the offensive-set even when no duplicates exist

**File:** `src/main/java/com/crims/effectiveinstruments/aura/InstrumentAuraMapping.java:723-745`

The audit walks every mapping × every allowed aura on every load. For 31 instruments × ~2 auras × the offensive-polarity check, that's ~62 registry lookups per `load()` call — fine for boot but redundant given the synthesis pass at lines 645-655 already builds the same `taken` set. Memoize:

```java
private static int auditOffensiveUniqueness() {
    Map<String, List<ResourceLocation>> offensiveOwners = computeOffensiveOwnership();
    // ... existing reporting logic
}

// Reused by synthesizeMissingOffensiveAllowed for its `taken` seed.
static Map<String, List<ResourceLocation>> computeOffensiveOwnership() { /* ... */ }
```

**Effort:** S — Impact: Low (boot-time perf, not steady-state).

---

### 3.4 Mobile fallback overlay shows mobile presets regardless of `showInSelector`

**File:** `src/main/java/com/crims/effectiveinstruments/client/event/AuraOverlayInjector.java:188-201`

```java
List<AuraPreset> allowed = MobileInstrumentAuraMapping.getAllowedAuras(imId);
if (allowed.isEmpty()) {
    allowed = new ArrayList<>();
    for (AuraPreset preset : AuraRegistry.getEnabledPresets()) {
        if (preset.supports(BuffTier.MOBILE)) {     // ← no showInSelector filter
            allowed.add(preset);
        }
    }
}
```

`MobileInstrumentAuraMapping.getAllowedAuras` deliberately doesn't filter by `showInSelector` (the doc at line 449 of that file explains why). But the fallback path here has different semantics — it's surfacing every mobile preset in the system, not just the curated set for this instrument. Hidden mobile presets (e.g., user-custom internal-only presets with `showInSelector=false`) will appear.

**Fix:** add the filter to the fallback specifically:

```java
if (preset.supports(BuffTier.MOBILE) && preset.showInSelector()) {
    allowed.add(preset);
}
```

**Effort:** S — Impact: Low (only matters for users with custom hidden mobile presets).

---

### 3.5 `AuraOverlayInjector.imOverlayScreen` can leak if `Closing` doesn't fire

**File:** `src/main/java/com/crims/effectiveinstruments/client/event/AuraOverlayInjector.java:412-438`

`onScreenClose` is the only place state is cleared. If the user uses `/say` or another keybind to swap to a non-`Closing`-firing screen path (rare, but Forge has had bugs here), the overlay state stays attached to a no-longer-rendered screen. The render and click handlers compare `event.getScreen() != imOverlayScreen` so they self-disable, but the state is stuck until next IM screen open.

**Fix:** add a defensive tick listener:

```java
@SubscribeEvent
public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    if (imOverlayScreen != null && Minecraft.getInstance().screen != imOverlayScreen) {
        // Screen swapped out without firing Closing — clean up.
        clearImState();
    }
}
```

(Or hook `ScreenEvent.Opening` and detect when a different screen takes over.)

**Effort:** S — Impact: Low (rare edge case, self-corrects on next IM open).

---

### 3.6 Protocol version isn't bumped after schema additions

**File:** `src/main/java/com/crims/effectiveinstruments/network/EIPacketHandler.java:14-17`

`PROTOCOL_VERSION = "4"` was set in 1.4.1. Since then:
- 1.4.3 added `mobileTier` + `close` fields to `InstrumentOpenC2SPacket` (covered by length-prefixed forward-compat).
- 1.4.5 changed which screen events the server expects.

The packet decoders are forward-compatible (they read optional bytes), but a 1.4.0 client could still send a packet missing the new fields and the server would default them to false — silently degrading the user experience. Bumping to "5" would surface a handshake mismatch to the user instead of silent degradation. Reasonable trade-off; defer until you ship a strictly incompatible change.

**Effort:** S — Impact: Low (today; could matter for 1.5.0).

---

### 3.7 `AuraJsonLoader` write helpers should share a base method

**File:** `src/main/java/com/crims/effectiveinstruments/aura/AuraJsonLoader.java`

Five near-identical writers exist:
- `writeDefaultJson` (704)
- `writeMobileDefault` (669)
- `writeOffensiveStationaryDefault` (581)
- `writeOffensiveMobileDefault` (604)
- `writeReadme` (736)

The first four all build the same JSON skeleton (`schemaVersion`, `displayName`, `description`, `color`, `enabled`, `durationTicks`, `radius`, `sortOrder`, then `tiers`, `showInSelector`, `effects`, `icon`, `iconSelected`). Centralize:

```java
private static void writePreset(Path dir, PresetSpec spec) throws IOException { ... }
record PresetSpec(String id, String displayName, String description, String color,
                  int durationTicks, int sortOrder, Set<BuffTier> tiers,
                  boolean showInSelector, Polarity polarity, String[][] effects,
                  @Nullable String iconPath, @Nullable String selectedIconPath) {}
```

Saves ~100 lines and prevents the next addition from drifting between the four writers (which already happened once between `writeOffensiveMobileDefault` and `writeMobileDefault` re: `showInSelector`).

**Effort:** M — Impact: Low (refactor, no user-facing change).

---

### 3.8 `OFFENSIVE_INCLUDE_OTHER_PLAYERS` is read in two paths

**File:** `src/main/java/com/crims/effectiveinstruments/aura/TargetingProfiles.java:39, 45`

The `if (OFFENSIVE_INCLUDE_ALL_NON_PETS) { include OTHER_PLAYER }` path on line 39 unconditionally adds other players. The `else { ... }` block on line 45 reads the dedicated `OFFENSIVE_INCLUDE_OTHER_PLAYERS` config. So with the default `includeAllNonPets=true`, the dedicated OTHER_PLAYERS toggle is ignored even though admins might expect it to gate PvP friendly-fire specifically.

This is documented as intentional in the comment, but it's still a config trap. A safer default: `OFFENSIVE_INCLUDE_ALL_NON_PETS` doesn't override `OFFENSIVE_INCLUDE_OTHER_PLAYERS` — the all-non-pets bit is "include everything in the per-category set", not "ignore the per-category set". Then admins who flip `OFFENSIVE_INCLUDE_OTHER_PLAYERS=false` get PvP-safe offensive auras even with `includeAllNonPets=true`.

**Fix:**

```java
if (OFFENSIVE_INCLUDE_ALL_NON_PETS.get()) {
    if (OFFENSIVE_INCLUDE_OTHER_PLAYERS.get()) allowed.add(OTHER_PLAYER);
    allowed.add(VILLAGER);
    allowed.add(IRON_GOLEM);
    allowed.add(PASSIVE_MOB);
    allowed.add(HOSTILE_MOB);
} else { /* ... */ }
```

**Effort:** S — Impact: Medium (PvP servers).

---

### 3.9 `AuraSelectorWidget` resize handling

**File:** `src/main/java/com/crims/effectiveinstruments/client/widget/AuraSelectorWidget.java:46-96`

Layout is computed in the constructor from `parentScreen.width`. On window resize, `Init.Post` re-fires (per AuraOverlayInjector docstring at line 110-115) so a fresh widget is built — but the *old* widget instance is still referenced by `selectorWidget` static. Then `setSelectedAuraId` calls go to the new widget (via the re-init path), but if anyone else holds the old reference (Forge GUI extras, mod overlays), they'll see stale state.

The class-level `parentScreen` field makes this kind of lifecycle issue easier to mishandle. Refactor: store `parentScreen` only in a local, expose only `setSelectedAuraId`/render through an interface, and have `AuraOverlayInjector` always read the current static.

**Effort:** S — Impact: Low (no in-tree caller holds a stale reference today).

---

### 3.10 `EICommands.runDiagnose` doesn't show positive-targeting state

**File:** `src/main/java/com/crims/effectiveinstruments/command/EICommands.java:163-171`

The diagnose command dumps every offensive-targeting toggle but only the master `OFFENSIVE_AURAS_ENABLED` of the offensive system. Positive-targeting per-category toggles are never shown. If a user reports "my regen aura doesn't hit my ally", the command can't help.

**Fix:** add a parallel block:

```java
source.sendSuccess(() -> Component.literal(
        "Positive targeting: otherPlayers=" + EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYERS.get()
                + " villagers=" + EIServerConfig.POSITIVE_INCLUDE_VILLAGERS.get()
                + " ironGolems=" + EIServerConfig.POSITIVE_INCLUDE_IRON_GOLEMS.get()
                + " passive=" + EIServerConfig.POSITIVE_INCLUDE_PASSIVE_MOBS.get()
                + " hostile=" + EIServerConfig.POSITIVE_INCLUDE_HOSTILE_MOBS.get()
), false);
```

**Effort:** S — Impact: Low (diagnostic completeness).

---

### 3.11 Unit tests cover JSON parsing, not state machines

**Existing tests:** `AuraApplicatorBehaviorTest` (172 lines), `AuraSchemaGateTest` (78), `AuraTierJsonTest` (154), `InstrumentAuraMappingJsonTest` (240), `MobileInstrumentAuraMappingJsonTest` (190), `OverwritePolicyTest` (61).

**Untested:**
- `AuraManager.PlayerAuraState.isActive` (sliding-window threshold logic).
- `InstrumentDurability.damage`/`repair`/`set` and the lazy-init contract for stacks without the NBT tag.
- `InstrumentAnvilHandler` repair math (combine + bonus + clamp).
- `EntityCategory.classify` decision tree (8 categories × tame/untame × extraPetTypes).
- `NoteActivityHandler.resolveCost` polarity multiplier.

The first three can be unit-tested against fakes in pure JUnit (no Minecraft needed for state-machine tests). `EntityCategory.classify` would need Mockito for `LivingEntity` instances or an in-memory fake. Adding ~150 lines of state-machine tests would catch a class of regression that the 1.4.x hotfixes fought hand-to-hand.

**Effort:** M — Impact: Medium (regression-resistance).

---

### 3.12 `InstrumentAuraMapping.load` log message under-reports synthesis

**File:** `src/main/java/com/crims/effectiveinstruments/aura/InstrumentAuraMapping.java:622`

```java
EffectiveInstrumentsMod.LOGGER.info("Loaded {} instrument-aura mappings", MAPPINGS.size());
```

The message fires after synthesis but reports only the count, not the augmentation. If 5 of 31 mappings were augmented in this load, the user has no visibility unless they grep for the `info`-level synthesis line above.

**Fix:**

```java
EffectiveInstrumentsMod.LOGGER.info(
    "Loaded {} instrument-aura mappings ({} synthesized in this load)",
    MAPPINGS.size(), synthesized);
```

**Effort:** S — Impact: Low.

---

## §4 — Architecture / Refactor (Bigger investments, optional)

### 4.1 Consolidate `EIServerConfig` into thematic sub-objects

**File:** `src/main/java/com/crims/effectiveinstruments/config/EIServerConfig.java`

The class has 38 public-static-final config values spread across one big class. The legacy/positive/mobile/offensive/durability sections logically map to nested configs. Forge's `ForgeConfigSpec` doesn't enforce nesting in Java, but you can split for readability:

```java
public final class EIServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final General GENERAL;
    public static final PositiveTargeting POSITIVE;
    public static final OffensiveTargeting OFFENSIVE;
    public static final Mobile MOBILE;
    public static final Durability DURABILITY;

    public static final class PositiveTargeting {
        public final ForgeConfigSpec.BooleanValue includeOtherPlayers;
        // ...
    }
}
```

Call-sites become `EIServerConfig.POSITIVE.includeOtherPlayers.get()` — slightly more verbose, but the static-import noise drops and IDE autocomplete narrows the search space.

**Effort:** M — Impact: Low (developer ergonomics).

---

### 4.2 Extract a `MarkerFile` utility

**Files:** `InstrumentAuraMapping.java`, `MobileInstrumentAuraMapping.java`, `InstrumentDurabilityConfig.java`, `AuraJsonLoader.java`

The marker-file pattern appears 7 times:

```java
Path marker = getXxxFile();
if (Files.exists(marker)) return;
// ... do migration ...
Files.createFile(marker);
```

Centralize:

```java
public static void runOnce(Path marker, IORunnable migration) throws IOException {
    if (Files.exists(marker)) return;
    Files.createDirectories(marker.getParent());
    migration.run();
    Files.createFile(marker);
}
```

Cleaner caller sites; the file-creation order on success/failure becomes consistent (today the marker-create happens before *or* after the work depending on the writer).

**Effort:** S-M — Impact: Low (developer ergonomics, slight bug-resistance).

---

### 4.3 Pull `PresetSpec` and `OffensiveDefault` out of `AuraJsonLoader`

The 1045-line `AuraJsonLoader` mixes:

1. The default-table data (`OFFENSIVE_DEFAULTS`, plus the inline calls in `ensureStationaryDefaults`).
2. JSON write helpers.
3. JSON parse helpers (`loadAll`, `parseFile`).

The data ought to live in a sibling `AuraDefaults` data class (or even a `defaults.json` resource that ships in the JAR) — separating "what we ship" from "how we serialize it" makes adding a new aura a 1-line data change instead of a 6-line writer call.

Going further: ship `defaults.json` as a JAR resource and have `AuraJsonLoader` copy-out-on-first-run rather than constructing JSON objects in code. That folds 600 lines of `writeXxxDefault` calls into a 5-line resource extractor.

**Effort:** L — Impact: Medium (significant code reduction, much easier to review aura content changes).

---

### 4.4 `AuraOverlayInjector` is doing too much

**File:** `src/main/java/com/crims/effectiveinstruments/client/event/AuraOverlayInjector.java`

499 lines, three responsibilities:
1. **Stationary path** (lines 117-147): widget injection into `InstrumentScreen`.
2. **IM render path** (lines 156-410): direct draw + click hit-testing on top of IM screens.
3. **Selection persistence** (lines 412-498): per-instrument-id session memory.

Split into:
- `StationaryAuraOverlay` — widget injection only.
- `MobileAuraOverlay` (or `ImmersiveMelodiesOverlay`) — render-event-based IM path.
- `AuraSelectionMemory` — the `instrumentAuraOverrides` map.

Each subscribes to its own slice of events. The current single-class approach is the legacy of 1.4.5's pivot from widget-based to render-based IM injection — the refactor was deferred to land the fix. Pay it back now.

**Effort:** L — Impact: Medium (regression-prevention; the 1.4.4→1.4.5 IM bug emerged because the responsibilities weren't separated).

---

### 4.5 Move `AuraRegistry`'s static state to a `RegistryManager` instance

`AuraRegistry`, `InstrumentAuraMapping`, `MobileInstrumentAuraMapping`, `InstrumentDurabilityConfig` all use static `private static final Map<...> X = new HashMap<>()` patterns. They all share the same lifecycle (load, reload). The static state makes unit tests painful (each test must reset all four classes) and prevents a future "reload only one of them" optimization.

A `RegistryManager` singleton with these as instance fields, accessed via a getter, would let tests instantiate fresh state per test. Combined with §4.6 below, you get full testability.

**Effort:** L — Impact: Medium (tests become much easier to write).

---

### 4.6 Make `EIServerConfig.X.get()` calls injectable

Currently every config read goes through static getters. Tests that touch `AuraApplicator.applyEffectSafely` need to mock `EIServerConfig.EFFECT_OVERWRITE_POLICY.get()` somehow — and the existing `AuraApplicatorBehaviorTest` does this via a custom config (presumably loading the spec at test start). A cleaner pattern: receive the policy as a parameter to `applyEffectSafely`, with the static-config read happening only at the entry point (`AuraManager.applyAuraEffects` and `ImmersiveMelodiesAuraHandler.tickPlayer`).

This is dependency injection done at the parameter level — no DI framework needed. Result: every leaf method becomes a pure function, trivially testable.

**Effort:** L — Impact: Medium (test ergonomics).

---

## §5 — New Feature Suggestions

These are forward-looking; pick what aligns with the mod's identity.

### 5.1 Per-player stationary aura selection persistence

**Pattern to follow:** `MobilePlayerSelection.java` already does this for the mobile tier — SavedData on the overworld, keyed by player UUID + instrument ID.

Today the stationary aura selection is session-only on the client (`AuraOverlayInjector.instrumentAuraOverrides:75`) and never persisted. Players who mostly use one instrument with one aura have to reselect after every server restart. Adding `StationaryPlayerSelection` parallel to `MobilePlayerSelection` would fix that.

**Tie-in:** the server-side default-aura auto-select (`AuraManager.onInstrumentIdReceived:145-162`) currently always picks the mapping's `default`. With persistence, it should prefer the player's saved selection if present.

**Effort:** M — Impact: High (UX win for repeat players).

---

### 5.2 Aura preset hot-reload (filesystem watcher)

Currently changes to `config/effective_instruments/auras/*.json` require `/effectiveinstruments reload`. A `WatchService` thread that triggers reload on change would let creators iterate on presets without command spam.

**Considerations:**
- Filesystem watch events fire at unpredictable cadence on different OSes; debounce to 1 second.
- Reload must happen on the main thread — use `MinecraftServer.execute(...)`.
- Disable on integrated server / single-player to avoid surprising the user.

**Effort:** M — Impact: Medium (creator-friendly).

---

### 5.3 In-game pet allowlist commands

`PET_ENTITY_ALLOWLIST` is config-only — admins must edit `server.toml` and reload. A pair of subcommands would be friendlier:

```
/effectiveinstruments pet add <entity_id>     [OP]
/effectiveinstruments pet remove <entity_id>  [OP]
/effectiveinstruments pet list
```

The `list` variant doesn't even need OP. Persist via the existing config or a dedicated SavedData.

**Effort:** S — Impact: Medium.

---

### 5.4 Diagnostic export for bug reports

Augment `/effectiveinstruments diagnose` with a `dump` subcommand that writes a timestamped JSON to `logs/ei-diagnose-<unixtime>.json`:

```json
{
  "timestamp": "2026-04-25T10:03:11Z",
  "mod_version": "1.4.8",
  "player": { "uuid": "...", "name": "...", "creative": false },
  "held_main": { ... },
  "held_off": { ... },
  "aura_state": { ... },
  "config": { /* every relevant config knob */ },
  "registry": { "presets_loaded": 62, "enabled": 60, "mappings": 31 }
}
```

Bug reports become unambiguous. Document the path in `CONTRIBUTING.md`.

**Effort:** S-M — Impact: High (slashes triage time).

---

### 5.5 Mending-style instrument enchantments

The durability NBT system already exposes a clean `damage`/`repair` API. Add custom enchantments:

- **Reverberate** (Mending equivalent): consume XP to repair durability when held + active aura.
- **Resonance** (Unbreaking equivalent): % chance to skip durability damage on a note.
- **Encore** (Aura radius extension): +25%/+50%/+75% radius per level.
- **Sustain** (Aura duration extension): +15%/+30%/+45% duration per level.

The first two work with vanilla XP/anvil flow; the latter two require feeding the level into `AuraPreset.getEffectiveRadius()` / `getEffectiveDuration()`. Stack-NBT lookup integrates cleanly with the existing `InstrumentDurability.resolveEntry` cache pattern.

**Effort:** L — Impact: High (significant gameplay surface area).

---

### 5.6 Generic music-event listener (extend beyond GI/EMI/IM)

`NoteActivityHandler` subscribes to GI's events. Other instrument mods (Bardic Inspirations, Magicraft, etc.) don't fire those. A tag-based detection layer:

1. Define a tag `effectiveinstruments:musical_instruments` (item-tag).
2. On any `PlayerInteractEvent.RightClickItem` for tagged items, treat as a "note played".
3. Custom JSON in mappings to bind tag-detected items to auras.

This widens the supported-mods set without per-mod compat code.

**Effort:** L — Impact: Medium-High (multiplies mod compatibility).

---

### 5.7 Aura preset stacking / mutual-exclusion semantics

When two musicians overlap, the LAST-applied wins (per `OverwritePolicy`). That's silently the case today; no UI conveys it. Define explicit stacking semantics per-aura:

- `"stack_mode": "highest_amplifier"` (current default behavior).
- `"stack_mode": "additive_amplifier"` (sum amplifiers up to a cap).
- `"stack_mode": "exclusive"` (first-applied locks the slot for N seconds).
- `"stack_mode": "additive_duration"` (refresh duration on overlap).

Surfaces as a JSON field on the preset. Useful for balance: support auras additive, debuff auras exclusive (no chain-Wither stacking).

**Effort:** M — Impact: Medium (balance + multiplayer experience).

---

### 5.8 REI/JEI plugin — instrument tooltip shows aura mapping

When the player hovers an instrument item in the recipe UI, show "Default aura: Zephyr's Blessing" + clickable list of allowed auras. REI integration via `me.shedaniel.rei.api.client.plugins.REIClientPlugin`; JEI via `mezz.jei.api.IModPlugin`.

Keep both as soft dependencies (compileOnly + ModList check at runtime).

**Effort:** M — Impact: Medium (discoverability).

---

### 5.9 Spectator-mode aura suppression

A spectator player playing an instrument (rare but possible via /gamemode change mid-play) currently still triggers the aura pipeline. Add a guard at `AuraManager.applyAuraNow` and `ImmersiveMelodiesAuraHandler.tickPlayer`:

```java
if (player.isSpectator()) return;
```

Belt-and-braces: reject the open packet too.

**Effort:** S — Impact: Low (rare scenario, but consistency-cost is zero).

---

### 5.10 Configurable selector position

Today the selector is hard-pinned to top-right. Some screens (notably some EMI variants) have UI in that corner. Add `EIClientConfig.SELECTOR_POSITION` enum (`TOP_RIGHT`, `TOP_LEFT`, `BOTTOM_RIGHT`, `BOTTOM_LEFT`) with a per-screen-class override map for power users.

**Effort:** S — Impact: Medium (UI conflicts).

---

### 5.11 Sound-volume tapering at low durability

A nice immersive touch: as durability approaches 0, the GI sound volume drops by up to 50%. Hook the volume-modifying event GI exposes (or post-process `InstrumentPlayedEvent`'s sound output via a Forge sound-event handler). The 1.4.6 low-durability action-bar warning already exists — this would be the auditory complement.

**Effort:** M — Impact: Low (atmosphere, not balance).

---

## §6 — Effort / Impact Reference

A quick at-a-glance shipping plan. Order by descending recommended priority for 1.4.9.

| # | Item | Effort | Impact | §  |
|---|---|---|---|---|
| 1 | Durability default-max fallback never fires | S | High | 1.1 |
| 2 | `AuraRegistry.load` runs at common-setup | M | High | 1.2 |
| 3 | Bar decorator unprotected SERVER-config read | S | Med | 1.3 |
| 4 | Mobile packet rate limits | S | Med | 1.4 |
| 5 | Stale `aura_hint` lang string | S | Med | 1.5 |
| 6 | Stale 1.3.0 free-play limitation comments/strings | S | Med | 2.1 |
| 7 | Stale `EntityCategory.classify` comment | S | Low | 2.2 |
| 8 | Wire deprecation warnings (Option A) | S | Low | 2.3 |
| 9 | Anvil work-penalty NBT escalation | S | Med | 2.4 |
| 10 | Atomic write parity | M | Low | 2.5 |
| 11 | IM tier active-set | M | Med | 2.6 |
| 12 | `affectedTargets` stale-entity prune | S | Low | 2.7 |
| 13 | Clamp `radius`/`durationTicks` at parse time | S | High* | 2.8 |
| 14 | Two icon generators | S | Low | 3.1 |
| 15 | `instrumentOpen` flag cleanup | S | Med | 3.2 |
| 16 | Mobile fallback overlay `showInSelector` filter | S | Low | 3.4 |
| 17 | Defensive screen-leak tick | S | Low | 3.5 |
| 18 | Diagnose: positive targeting block | S | Low | 3.10 |
| 19 | State-machine unit tests | M | Med | 3.11 |
| 20 | Synthesis log message | S | Low | 3.12 |
| — | **§4 — Architecture refactors** | M-L | Med | 4.* |
| — | **§5 — New features** | varies | varies | 5.* |

\* High *given* a hostile JSON; low under normal use. Cheap fix, big downside avoidance — easy yes.

A reasonable 1.4.9 cut: items **1, 3, 4, 5, 6, 9, 13, 15, 16, 18** (every S-effort fix above). Items **2, 11, 19** are the M-effort bumps that earn their cost.

For 1.5.0 plan items **§4.3** (default JSON resource), **§4.4** (overlay split), and **§5.1** (stationary persistence) — each is a meaningful feature-or-architecture step, all manageable in a single milestone.

---

## §7 — Things deliberately NOT recommended

A few patterns worth calling out as fine-as-is, in case future readers wonder:

- **Static singletons everywhere.** `AuraManager`, `AuraRegistry`, etc. are static-state classes. This is idiomatic Forge mod code and Minecraft's own systems work the same way. Refactoring (§4.5) is for testability, not "best practice".
- **Reflection-free IM compat.** The NBT-key + registry-id detection pattern in `ImmersiveMelodiesCompat` is brittle in theory but cheaper and safer than `Class.forName` + reflective method invocation. Keep it.
- **Per-handler `Map<UUID, Long>` throttles.** The repetition (`LAST_BROKEN_TICK`, `LAST_LOW_TICK`, the per-packet rate limits) looks like duplication, but cleaning up logout-leak across many maps means many subtle race opportunities. The current pattern is fine.
- **Synchronous JSON IO on the main thread.** `AuraRegistry.load` blocks the server thread for ~200ms during reload. For a config-load triggered by a command, that's acceptable. Don't refactor to async unless you have profiling data showing it matters.

---

*End of report.*
