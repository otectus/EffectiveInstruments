# Effective Instruments

A Minecraft Forge 1.20.1 add-on mod for [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) that adds an **aura while playing** system. Select a positive (buff) or negative (debuff) aura on any instrument screen — while you play notes, nearby allies receive beneficial potion effects, or nearby mobs take debuffs.

## Features

- **Dual-polarity auras** — every instrument has a positive (support) aura *and* a negative (offensive) aura. Pick the one you want on each play session via the selector.
- **Instrument-specific auras, 1-to-1 unique** — 42 instruments across Genshin Instruments, Even More Instruments, and Immersive Melodies, each with its own distinct positive + offensive preset (84 unique auras total, 88 including user-custom ones).
- **Instrument durability** — instruments wear out with use (per-instrument NBT tag, no item-class modification needed) and can be repaired on an anvil with the configured material. Vanilla-style durability bar overlays every instrument slot. Creative-mode immunity is configurable.
- **Dual-tier system** — stationary tier for screen-based instruments (GI / EMI) and passive mobile tier for Immersive Melodies playback (autoplay + free-play).
- **Broad offensive targeting by default** — offensive auras hit every mob in range except the musician, their own pets, and other players' pets. Per-category fine-grained knobs remain available for admins.
- **Per-instrument memory** — manual aura overrides are remembered per-instrument within your session.
- **Server-authoritative** — all effects applied server-side, no cheating.
- **Automatic note detection** — hooks into Genshin Instruments' event API (both `NoteSoundPlayedEvent` and `HeldNoteSoundPlayedEvent`), no extra key presses needed.
- **Fully configurable** — radius, timing, targeting rules, durability, and per-aura overrides for both tiers.
- **Compatible with Even More Instruments** — works on all instrument screens from both mods out of the box.
- **Optional Immersive Melodies compatibility** — passive buffs while walking around playing IM instruments, no hard dependency.

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

> **Note:** Mobile buffs only activate during autoplay / selected-melody playback. Free-play keyboard/MIDI mode in Immersive Melodies is not supported in this version.

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

| Dependency | Version | Required? |
|------------|---------|-----------|
| Minecraft | 1.20.1 | Yes |
| Forge | 47.3.0+ | Yes |
| Genshin Instruments | 5.0+ | Yes |
| Even More Instruments | 6.0+ | No (optional) |
| Immersive Melodies | 0.6.0+ | No (optional — enables mobile tier) |

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

- Subscribes to Genshin Instruments' `InstrumentPlayedEvent` for note detection — no custom note packets, no mixins
- Subscribes to `InstrumentOpenStateChangedEvent` for instrument open/close tracking
- Uses `InstrumentScreen.getInstrumentId()` to identify which instrument is being played
- Three custom packets: `SelectAuraC2SPacket` (aura selection), `InstrumentOpenC2SPacket` (instrument ID), `SyncAuraSelectionS2CPacket` (server→client default sync)
- Client overlay injected via `ScreenEvent.Init.Post` using `event.addListener()`
- All Even More Instruments screens extend GI's `InstrumentScreen`, so `instanceof` detection covers both mods automatically
- Night Vision aura uses 260-tick duration to avoid the vanilla flicker warning
- **Immersive Melodies bridge** reads only `ModList.isLoaded` + vanilla `ItemStack` NBT (`playing` boolean) + Forge item registry — no IM classes imported, no reflection, no mixins. When IM is absent, the compat layer is a zero-cost no-op
- **Dual-tier architecture**: shared `AuraApplicator` handles effect application, target gathering, and cleanup for both tiers. Mobile tier uses its own config group and separate mapping file
- **Durability via NBT on foreign items**: the mod doesn't own any instrument `Item` classes, so `maxDamage` can't be set. Instead, durability is tracked under a custom NBT tag (`EIDurability`) with a `IItemDecorator` client-side for the visual bar

## License

MIT
