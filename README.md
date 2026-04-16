# Effective Instruments

A Minecraft Forge 1.20.1 add-on mod for [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) that adds an **aura while playing** system. Select a buff aura on any instrument screen — while you play notes, nearby allies receive beneficial potion effects.

## Features

- **Instrument-specific auras** — each instrument can have its own default aura that auto-selects when opened
- **26 unique aura presets** — 15 stationary (Genshin/EMI) + 11 mobile (Immersive Melodies), fully data-driven, add your own via JSON
- **Dual-tier system** — stationary tier for screen-based instruments (GI/EMI) and mobile passive tier for Immersive Melodies autoplay
- **Per-instrument memory** — manual aura overrides are remembered per-instrument within your session
- **Server-authoritative** — all effects applied server-side, no cheating
- **Automatic note detection** — hooks into Genshin Instruments' event API, no extra key presses needed
- **Fully configurable** — radius, timing, targeting rules, and per-aura overrides for both tiers
- **Compatible with Even More Instruments** — works on all instrument screens from both mods out of the box
- **Optional Immersive Melodies compatibility** — passive buffs while walking around playing IM instruments, no hard dependency

## Aura Presets

### Stationary Tier (Genshin Instruments / Even More Instruments)

Each instrument has its own unique aura. Effects are refreshed as long as you keep playing on the instrument screen.

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

Note Block Instrument variants are also mapped to thematically matching auras.

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
3. Play notes — allies within 16 blocks (configurable) receive the selected buff
4. Click a different aura button to override, or click the active aura to deselect
5. Close the instrument — aura clears, effects expire naturally

**Who receives buffs:**
- Yourself (toggleable)
- Other players in range (toggleable)
- Your tamed animals in range (toggleable) — wolves, cats, horses, parrots, etc.

Effects use a **strongest-wins** policy by default: if an ally already has a stronger version of the same effect (e.g. Resistance II from a Beacon), the aura will not downgrade it. Pack authors can change this via `effectOverwritePolicy` in the server config.

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

#### Targeting
| Option | Default | Description |
|--------|---------|-------------|
| `allowSelfBuff` | `true` | Musician receives their own aura |
| `includeOtherPlayers` | `true` | Other players in range receive effects |
| `includeTamedPets` | `true` | Tamed animals in range receive effects |
| `maxTargetsPerTick` | `32` | Hard cap on entities buffed per musician per tick |
| `petEntityTypeAllowlist` | `[]` | Extra entity type IDs to treat as pets |

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
- See [INSTRUMENT_AURAS.md](INSTRUMENT_AURAS.md) for the full design reference with rationale for each aura's effects and color choices

## License

MIT
