# Effective Instruments

A Minecraft Forge 1.20.1 mod that adds an **aura while playing** system to supported instrument backends. Select a positive (buff) or negative (debuff) aura — while you play notes, nearby allies receive beneficial potion effects, or nearby mobs take debuffs.

As of 1.5.0, both [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) (stationary screen-based instruments) and [Immersive Melodies](https://www.curseforge.com/minecraft/mc-mods/immersive-melodies) (mobile passive instruments) are **optional backends**. Install either one — or both — and Effective Instruments adapts. Without either, the mod loads cleanly, logs a warning, and waits.

## New in 1.6.0 — Multi-mod NPC compatibility

Effective Instruments now generalizes from a player-only aura framework to a *generic LivingEntity* framework. Supported NPC mods can drive the aura pipeline themselves: hand a Touhou Maid a Windsong Lyre and she'll play it and apply the aura; equip a Recruits soldier with a flute and he'll perform between combats; an MCA villager can serenade her spouse.

**Supported NPC mods (9 adapters, all optional):**

| Mod | Tier | What it does |
|---|---|---|
| Recruits (`recruits`) | 1 | Plays during idle; reflection-cached owner / inventory / fleeing / should-rest gates |
| Guard Villagers (`guardvillagers`) | 1 | Plays when a Hero of the Village player is nearby; combat-priority 5 |
| Easy NPC (`easy_npc`) | 1 | `EasyNPC` marker interface; owner via `OwnableEntity` vanilla path |
| Doggy Talents Next (`doggytalents`) | 1 | OFFHAND binding; sitting OR docile-mode gate |
| Iron's Spells summons (`irons_spellbooks`) | 1 | `IMagicSummon.getSummoner()` for owner; timer-bound performance |
| Ars Nouveau Starbuncle (`ars_nouveau`) | 1 | Plays when idle (ownerless — no friend/foe split based on tamer) |
| Touhou Little Maid (`touhou_little_maid`) | 1 | Goal-based; reads `IMaidTask.getUid()` for combat veto |
| MCA Reborn (`mca`) | 1 | Spouse-as-owner classification + plays instruments fully |
| Pehkui (`pehkui`) | library | Multiplies non-player aura radius by `ScaleTypes.BASE` scale |

Plus 21 curated entity-classification entries shipped for long-tail mods (Alex's Mobs, Friends & Foes, Twilight Forest, Cataclysm, Mowzie's Mobs, …) — see [`docs/npc-compat.md`](docs/npc-compat.md) for the per-mod cheat sheet.

**Modpack authors:** every classification is overridable via `data/effective_instruments/tags/entity_types/` tags or `config/effective_instruments/entity_classification.json`. Eight tags ship: `always_buff`, `always_debuff`, `ignore`, `force_performer`, `never_performer`, `treat_as_villager`, `treat_as_iron_golem`, `player_proxy_owner`.

**Server admins:** `/effectiveinstruments npcs adapters | list [radius] [all] | diagnose <entity>` exposes the full runtime state of every NPC adapter.

**Player path unchanged.** Vanilla worlds with EI + GI + IM see byte-identical aura behavior to 1.5.0 — gated by the new `AuraBehaviorParityTest` regression test.

**Architecture invariant:** the `checkCompatAuditInvariant` gradle task fails the build if any class outside `compat/<modid>/` imports the target mod's runtime types. Each adapter is fully quarantined; reflection-defensive runtime keeps the mod loading cleanly when any NPC mod is absent.

## Features

- **Dual-polarity auras** — every instrument has a positive (support) aura *and* a negative (offensive) aura. Pick the one you want on each play session via the selector.
- **Instrument-specific auras, 1-to-1 unique** — 42 instruments across Genshin Instruments, Even More Instruments, and Immersive Melodies, each with its own distinct positive + offensive preset (84 unique auras total, 88 including user-custom ones).
- **Instrument durability** — instruments wear out with use (per-instrument NBT tag, no item-class modification needed) and can be repaired on an anvil with the configured material. Vanilla-style durability bar overlays every instrument slot. Creative-mode immunity is configurable.
- **Dual-tier system** — stationary tier for screen-based instruments (GI / EMI) and passive mobile tier for Immersive Melodies playback (autoplay + free-play).
- **Broad offensive targeting by default** — offensive auras hit every mob in range except the musician, their own pets, and other players' pets. Per-category fine-grained knobs remain available for admins.
- **Per-instrument memory** — manual aura overrides are remembered per-instrument within your session.
- **Server-authoritative** — all effects applied server-side, no cheating.
- **Automatic note detection** — hooks into the active backend's event API per-side (Genshin Instruments' `NoteSoundPlayedEvent` / `HeldNoteSoundPlayedEvent`, or the Immersive Melodies NBT/screen-open path). No extra key presses needed.
- **Fully configurable** — radius, timing, targeting rules, durability, and per-aura overrides for both tiers.
- **Compatible with Even More Instruments** — works on all EMI instrument screens (which extend Genshin Instruments) automatically.
- **Optional backends** — Genshin Instruments and Immersive Melodies are both optional in 1.5.0. Run with either, both, or neither (the mod loads quietly with no backend, and `/effectiveinstruments diagnose` tells you what's installed).

## Aura Presets

Every instrument ships with *two* unique presets: a positive (support) aura shown with a green border, and an offensive (debuff) aura shown with a red border. Pick between them via the selector icons in the top-right of the instrument screen.

### Stationary Tier — Positive (Genshin Instruments / Even More Instruments)

Each instrument has its own unique positive aura. Effects are refreshed as long as you keep playing on the instrument screen (or for up to 5 seconds after the last note).

| Instrument | Aura | Effects |
|---|---|---|
| Windsong Lyre | Zephyr's Blessing | Speed I |
| Vintage Lyre | Echoes of Antiquity | Regeneration I |
| Floral Zither | Bloom Veil | Absorption I + Saturation I |
| Glorious Drum | Warcry Cadence | Strength I + Resistance I |
| Nightwind Horn | Moonlit Passage | Night Vision + Slow Falling |
| Ukulele | Sunkissed Serenade | Luck I |
| Djem Djem Drum | Rhythm of the Earth | Haste I + Jump Boost I |
| Guitar | Wanderer's Anthem | Speed I + Jump Boost I |
| Keyboard | Harmonic Resonance | Regeneration I + Haste I |
| Koto | Tranquil Current | Water Breathing + Dolphin's Grace |
| Pipa | Silk Road Vigor | Speed I + Strength I |
| Saxophone | Smoky Allure | Hero of the Village I |
| Shamisen | Ghost Flame | Fire Resistance + Strength I |
| Trombone | Bulwark Fanfare | Resistance I + Absorption II |
| Violin | Heartstring Aria | Regeneration I + Absorption I |

Every EMI Note Block Instrument variant (harp, bass, basedrum, snare, hat, bell, chime, flute, guitar, xylophone, iron_xylophone, cow_bell, didgeridoo, bit, banjo, pling) also has its own unique positive aura — 16 more presets covering the full set. The full table lives in the generated `_README_INSTRUMENTS.txt` in your config directory and in the per-preset JSONs under `config/effective_instruments/auras/`.

### Stationary Tier — Offensive (all instruments)

Every instrument also has a unique offensive mirror. Offensive auras target every mob in range except pets (by default; see `offensiveTargeting` config). Selected offensive presets show a red border; affected mobs display standard (non-ambient) debuff particles so the hit lands visibly.

Examples:

| Instrument | Offensive Aura | Effects |
|---|---|---|
| Windsong Lyre | Zephyr's Wrath | Slowness II + Weakness I |
| Vintage Lyre | Echoes of Decay | Wither I + Mining Fatigue I |
| Floral Zither | Withering Bloom | Poison II + Weakness I |
| Glorious Drum | Battle Fanfare | Weakness II + Slowness I |
| Nightwind Horn | Ebon Dirge | Blindness + Nausea |
| Ukulele | Mischief Melody | Blindness + Poison I |
| Djem Djem Drum | Earth Tremor | Slowness I + Mining Fatigue II |

The full list of 31 offensive presets + 11 mobile offensive presets lives in `config/effective_instruments/auras/` after first launch. Every offensive id is 1-to-1 unique with an instrument — no duplicates across the shipped mapping.

> **Server Admin Note:** The *Smoky Allure* aura (Saxophone) grants Hero of the Village, which gives significant trade discounts from villagers. If this is too powerful for your server's economy, you can disable it by setting `"enabled": false` in `config/effective_instruments/auras/smoky_allure.json` and running `/effectiveinstruments reload`.

### Mobile Tier (Immersive Melodies)

When [Immersive Melodies](https://www.curseforge.com/minecraft/mc-mods/immersive-melodies) is installed, players holding a playing IM instrument receive a lighter, passive aura buff. Mobile auras are intentionally single-effect and shorter range/duration than stationary auras for balance. If both tiers apply to the same player, the stationary tier takes precedence (configurable).

| IM Instrument | Passive Aura | Effect |
|---|---|---|
| Flute | Windstep | Speed I |
| Lute | Traveler's Hum | Luck I |
| Piano | Measured Tempo | Haste I |
| Vielle | Hearthsong | Regeneration I |
| Didgeridoo | Earthpulse | Jump Boost I |
| Bagpipe | Steadfast Drone | Resistance I |
| Trumpet | Brass Call | Strength I |
| Tiny Drum | March Tap | Speed I |
| Triangle | Clear Ping | Night Vision |
| Handpan | Stillwater | Water Breathing |
| Ender Bass | Shade Resonance | Fire Resistance |

Mobile mappings live in `config/effective_instruments/mobile_instrument_auras.json` — same reload workflow as the stationary mappings.

> **Note:** Mobile auras fire during autoplay, selected-melody playback, and free-play (any open IM screen). Durability is only charged on the held-and-playing path — browsing the melody list is free.

## How It Works

1. Open any instrument screen (from Genshin Instruments or Even More Instruments)
2. The instrument's default aura auto-selects (if configured)
3. Play notes — allies (positive) or mobs (offensive) within 16 blocks (configurable) are affected
4. Click a different aura icon to switch, or click the active aura to deselect
5. Close the instrument — aura lingers briefly then clears

**Positive aura targeting (who receives buffs):**
- Yourself (always — polarity-enforced)
- Your own tamed animals (always — polarity-enforced)
- Other players in range (toggleable)
- Other players' pets, villagers, iron golems, passive mobs, hostile mobs (each toggleable)

**Offensive aura targeting (who receives debuffs):**
- Default: everything in range **except** the musician, their own pets, and other players' pets
- Admins who want fine-grained per-category control set `offensiveTargeting.includeAllNonPets = false` and toggle the individual include-knobs

Effects use a **strongest-wins** policy by default: if a target already has a stronger version of the same effect (e.g. Resistance II from a Beacon), the aura will not downgrade it. Pack authors can change this via `effectOverwritePolicy` in the server config.

## Instrument Durability

Every tracked instrument has an NBT-backed durability counter. Playing notes (stationary tier) or pulses (mobile tier) consumes durability; offensive auras consume it faster by a configurable multiplier. At 0 durability the instrument is "broken" — note events are canceled with a chat warning.

**Repair** via anvil:
- Combine two damaged copies for the vanilla-style +12% max bonus.
- Or consume the instrument's configured repair material (e.g. string, gold, bamboo) for `repairPerUnit` durability each.

**Visual feedback:** a vanilla-style durability bar renders at the bottom of every tracked instrument's item slot. Hidden at full, green→yellow→red as durability drops. Low-durability warning fires at 10% remaining; broken-instrument warning fires when the note is canceled.

**Creative immunity:** on by default (matches vanilla tool behavior). Flip `durability.creativeImmunity = false` to verify depletion in creative mode.

Defaults live in `config/effective_instruments/instrument_durability.json` (max durability per instrument id, repair material, repair-per-unit). Admin subcommand: `/effectiveinstruments durability {get|set <n>|repair}`.

## Instrument-Aura Mapping

Each instrument can be mapped to specific auras in `config/effective_instruments/instrument_auras.json`. When a player opens an instrument, only its allowed auras appear in the selector, and the default aura is auto-selected.

**Default mappings are generated on first launch** — every GI and EMI instrument is mapped to its unique aura (see the aura table above). All 16 Note Block Instrument variants are also mapped.

**Adding more auras to an instrument:** Change the string shorthand to the object form:

```json
{
  "genshinstrument:windsong_lyre": {
    "default": "zephyrs_blessing",
    "allowed": ["zephyrs_blessing", "echoes_of_antiquity", "moonlit_passage"]
  }
}
```

- **String form** (`"instrument": "aura"`) — single aura, default and only option
- **Object form** — `"default"` is auto-selected on open, `"allowed"` lists all auras shown in selector
- **Unmapped instruments** show all auras (backwards-compatible)

See `_README_INSTRUMENTS.txt` in the config folder for all instrument IDs.

**Per-instrument memory:** If you manually select a different aura, that override is remembered for that specific instrument within your session (lost on logout).

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/effectiveinstruments reload` | OP (level 2) | Reload all aura presets and instrument mappings |
| `/effectiveinstruments status [player]` | OP (level 2) | Show aura state for a player |
| `/effectiveinstruments durability {get\|set <n>\|repair}` | OP (level 2) | Inspect or edit held instrument durability |
| `/effectiveinstruments diagnose` | Everyone | Dump pipeline state (answers "why isn't my aura firing?") |
| `/effectiveinstruments reset-mappings` | OP (level 2) | Delete + regenerate the instrument-aura mapping from the canonical table |
| `/effectiveinstruments help` | Everyone | Print config location and command summary |

## Requirements

| Dependency | Version | Required? | Purpose |
|------------|---------|-----------|---------|
| Minecraft | 1.20.1 | Yes | Base game |
| Forge | 47.3.0+ | Yes | Mod loader |
| Genshin Instruments | 5.0+ | No | Enables stationary instrument-screen aura support |
| Even More Instruments | 6.0+ | No | Adds more Genshin-style stationary instruments (requires GI) |
| Immersive Melodies | 0.6.0+ | No | Enables mobile/passive instrument aura support |

> Effective Instruments requires **at least one supported instrument backend** to provide gameplay functionality. Install Genshin Instruments for stationary screen-based instruments, Immersive Melodies for mobile instruments, or both. Without either, the mod loads but does nothing in-game; `/effectiveinstruments diagnose` will report `Backends: genshin=absent immersive_melodies=absent`.

## Configuration

Config files are generated on first launch in `config/effective_instruments/`.

### Server Config (`effective_instruments/server.toml`)

#### General
| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `enabled` | `true` | — | Master enable/disable |
| `noteWindowTicks` | `100` | 20–600 | Ticks after last note before aura deactivates (100 = 5s) |
| `auraTickIntervalTicks` | `10` | 1–100 | How often effects are refreshed |
| `defaultRadius` | `16` | 1–64 | Aura range in blocks |
| `effectOverwritePolicy` | `REFRESH_TIES` | enum | How new effects interact with pre-existing ones (see below) |
| `noteThresholdMin` | `1` | 1–32 | Minimum notes in the sliding window to activate the aura |
| `noteThresholdWindowTicks` | `40` | 1–600 | Size of the sliding window for `noteThresholdMin` |
| `debugMode` | `false` | — | Emit per-tick diagnostics (noisy, keep off in production) |

**`effectOverwritePolicy` values:**
- `NEVER_OVERWRITE` — only apply when target has no effect of this type
- `STRONGER_ONLY` — overwrite only if new amplifier is strictly greater
- `REFRESH_TIES` — overwrite if new amplifier ≥ existing *(default; legacy behavior)*
- `ALWAYS` — always overwrite regardless of existing amplifier

#### Targeting — Positive (`positiveTargeting`)

Musician-self and own-pets are always included (polarity-enforced).

| Option | Default | Description |
|--------|---------|-------------|
| `includeOtherPlayers` | `true` | Other players in range receive effects |
| `includeOtherPlayerPets` | `true` | Other players' tamed pets receive effects |
| `includeVillagers` | `true` | Villagers & wandering traders receive effects |
| `includeIronGolems` | `true` | Iron golems receive effects |
| `includePassiveMobs` | `false` | Passive mobs (cows, sheep, etc.) receive effects |
| `includeHostileMobs` | `false` | Hostile mobs receive effects (usually a trap) |
| `maxTargetsPerTick` | `32` | Hard cap on entities buffed per musician per tick |

#### Targeting — Offensive (`offensiveTargeting`)

Musician-self and own-pets are always excluded (polarity-enforced).

| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `true` | Master toggle for negative-polarity auras |
| `includeAllNonPets` | `true` | Broad mode: hit every mob except pets, ignoring per-category knobs below |
| `includeOtherPlayers` | `true` | (fine-grained) Other players receive debuffs |
| `includeOtherPlayerPets` | `false` | (fine-grained) Other players' pets receive debuffs |
| `includeVillagers` | `false` | (fine-grained) Villagers receive debuffs |
| `includeIronGolems` | `false` | (fine-grained) Iron golems receive debuffs |
| `includePassiveMobs` | `false` | (fine-grained) Passive mobs receive debuffs |
| `includeHostileMobs` | `true` | (fine-grained) Hostile mobs receive debuffs |
| `maxTargetsPerTick` | `32` | Hard cap on entities debuffed per musician per tick |
| `durabilityCostMultiplier` | `2` | Offensive play wears the instrument N× faster |

`petEntityTypeAllowlist` (list of entity type IDs) stays on the legacy `targeting` block — extends the pet classifier beyond `TamableAnimal` / `AbstractHorse`.

#### Durability (`durability`)

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `enabled` | `true` | — | Master toggle for durability + broken-state gating |
| `creativeImmunity` | `true` | — | Creative players' instruments don't wear out (flip to `false` to verify depletion) |
| `costPerNote` | `1` | 1–8 | Durability consumed per stationary note |
| `costPerMobilePulse` | `1` | 1–8 | Durability consumed per mobile pulse |
| `defaultMax` | `1200` | 1–100000 | Fallback max when an instrument is missing from `instrument_durability.json` |
| `anvilCombineBonusPercent` | `12` | 0–100 | Vanilla-style bonus when combining two damaged copies |

#### Mobile Tier (Immersive Melodies)

These settings are under `[mobileTier]` and only take effect when Immersive Melodies is installed. When IM is absent, they are harmless no-ops.

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `enabled` | `true` | — | Master toggle for mobile tier |
| `pulseIntervalTicks` | `20` | 5–100 | How often (in ticks) mobile effects refresh (20 = 1s) |
| `lingerTicks` | `60` | 0–200 | How long effects linger after the player stops playing |
| `defaultRadius` | `8` | 1–32 | Mobile aura range when JSON radius is `-1` |
| `maxTargetsPerTick` | `16` | 1–256 | Entity cap per mobile musician per pulse |
| `allowSelfBuff` | `true` | — | Musician receives their own mobile effects |
| `includeOtherPlayers` | `true` | — | Other players in range receive mobile effects |
| `includeTamedPets` | `false` | — | Tamed pets in range receive mobile effects (off by default) |
| `suppressWhenStationaryActive` | `true` | — | Stationary tier suppresses mobile for the same player |

### Client Config (`effective_instruments/client.toml`)

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `showOverlay` | `true` | — | Show aura selector on instrument screens |
| `overlayScale` | `1.0` | 0.5–2.0 | Button scale factor |
| `compactMode` | `false` | — | Smaller button layout |
| `particlesMode` | `ALL` | ALL / MINIMAL / NONE | Particle effects when aura is active |
| `reducedMotion` | `false` | — | Dampen aura particle drift and pulse (accessibility) |
| `screenClassAllowlist` | `[]` | — | Fallback class names for non-GI instrument screens |

## Building from Source

Requires **Java 17**. No manual dependency setup — Genshin Instruments
and Even More Instruments are resolved from [Curse Maven](https://www.cursemaven.com/)
on first build.

```bash
./gradlew build
```

The built JAR will be at `build/libs/effectiveinstruments-<version>.jar`.
Use `./gradlew runClient` for a dev client and `./gradlew test` for
unit tests.

When bumping a dependency version, update both the version string and
the matching `_file_id` property in `gradle.properties`. File IDs are
visible at `https://www.curseforge.com/minecraft/mc-mods/<slug>/files`
(numeric id in the download URL).

To launch a dev client with Genshin Instruments present at runtime
(the dependency is `compileOnly` since 1.5.0):

```bash
./gradlew runClient -PdevRuntimeGenshin=true
```

## Integration for Third-Party Instrument Mods

The package `com.crims.effectiveinstruments.api` contains the stable
integration surface. Third-party instrument mods can drive aura
lifecycle events without depending on Genshin Instruments:

```java
EffectiveInstrumentsAPI.notifyInstrumentOpen(player);
EffectiveInstrumentsAPI.notifyInstrumentIdReceived(player, myInstrumentId);
EffectiveInstrumentsAPI.notifyNotePlayed(player);
// ... later:
EffectiveInstrumentsAPI.notifyInstrumentClose(player);
```

Only classes under `.api` are part of the supported integration surface.
Everything else is internal and may change between releases.

## Technical Details

- **Optional backend quarantine (1.5.0)** — Genshin Instruments is loaded only when `ModList.get().isLoaded("genshinstrument")` returns true at common-setup. The GI event handler (`compat/genshin/GenshinInstrumentEventHandler`) is the only class that imports `com.cstav.genshinstrument`; it is registered manually on the Forge event bus by the compat bootstrap. The client-side `GenshinInstrumentScreenBridge` uses `Class.forName` + cached `Method` lookups to detect GI instrument screens and read `getInstrumentId()` without compile-time references to GI types. Result: zero `NoClassDefFoundError` risk when GI is absent.
- Subscribes to Genshin Instruments' `NoteSoundPlayedEvent` and `HeldNoteSoundPlayedEvent` for note detection — no custom note packets, no mixins.
- Subscribes to `InstrumentOpenStateChangedEvent` for instrument open/close tracking.
- Three custom packets: `SelectAuraC2SPacket` (aura selection), `InstrumentOpenC2SPacket` (instrument ID), `SyncAuraSelectionS2CPacket` (server→client default sync). The wire protocol version is unchanged in 1.5.0.
- Client overlay injected via `ScreenEvent.Init.Post` using `event.addListener()`.
- All Even More Instruments screens extend GI's instrument screen base class, so the GI-screen-bridge detection covers EMI when GI is also installed.
- Night Vision aura uses 260-tick duration to avoid the vanilla flicker warning.
- **Backend-agnostic note pipeline** — `StationaryInstrumentNoteService` owns the broken-state gate, polarity-aware durability damage, aura record + immediate-apply, and the per-player broken/low-durability message throttles. The GI event handler is a thin adapter that unwraps the GI event into a `ServerPlayer` + optional instrument id and delegates here.
- **Immersive Melodies bridge** reads only `ModList.isLoaded` + vanilla `ItemStack` NBT (`playing` boolean) + Forge item registry — no IM classes imported, no reflection, no mixins. When IM is absent, the compat layer is a zero-cost no-op.
- **Dual-tier architecture**: shared `AuraApplicator` handles effect application, target gathering, and cleanup for both tiers. Mobile tier uses its own config group and separate mapping file.
- **Durability via NBT on foreign items**: the mod doesn't own any instrument `Item` classes, so `maxDamage` can't be set. Instead, durability is tracked under a custom NBT tag (`EIDurability`) with a `IItemDecorator` client-side for the visual bar.

## License

MIT
