# Effective Instruments

A Minecraft Forge 1.20.1 add-on mod for [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) that adds an **aura while playing** system. Select a buff aura on any instrument screen — while you play notes, nearby allies receive beneficial potion effects.

## Features

- **Instrument-specific auras** — each instrument can have its own default aura that auto-selects when opened
- **15 unique aura presets** — fully data-driven, add your own via JSON
- **Per-instrument memory** — manual aura overrides are remembered per-instrument within your session
- **Server-authoritative** — all effects applied server-side, no cheating
- **Automatic note detection** — hooks into Genshin Instruments' event API, no extra key presses needed
- **Fully configurable** — radius, timing, targeting rules, and per-aura overrides
- **Compatible with Even More Instruments** — works on all instrument screens from both mods out of the box

## Aura Presets

Each instrument has its own unique aura. Effects are refreshed as long as you keep playing.

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

Effects use a **strongest-wins** policy: if an ally already has a stronger version of the same effect (e.g. Resistance II from a Beacon), the aura will not downgrade it.

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

## Requirements

| Dependency | Version | Required? |
|------------|---------|-----------|
| Minecraft | 1.20.1 | Yes |
| Forge | 47.3.0+ | Yes |
| Genshin Instruments | 5.0+ | Yes |
| Even More Instruments | 6.0+ | No (optional) |

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

#### Targeting
| Option | Default | Description |
|--------|---------|-------------|
| `allowSelfBuff` | `true` | Musician receives their own aura |
| `includeOtherPlayers` | `true` | Other players in range receive effects |
| `includeTamedPets` | `true` | Tamed animals in range receive effects |
| `petEntityTypeAllowlist` | `[]` | Extra entity type IDs to treat as pets |

### Client Config (`effective_instruments/client.toml`)

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `showOverlay` | `true` | — | Show aura selector on instrument screens |
| `overlayScale` | `1.0` | 0.5–2.0 | Button scale factor |
| `compactMode` | `false` | — | Smaller button layout |
| `particlesMode` | `ALL` | ALL / MINIMAL / NONE | Particle effects when aura is active |
| `screenClassAllowlist` | `[]` | — | Fallback class names for non-GI instrument screens |

## Building from Source

Requires **Java 17**.

```bash
./gradlew build
```

The built JAR will be at `build/libs/effectiveinstruments-<version>.jar`.

## Technical Details

- Subscribes to Genshin Instruments' `InstrumentPlayedEvent` for note detection — no custom note packets, no mixins, no reflection
- Subscribes to `InstrumentOpenStateChangedEvent` for instrument open/close tracking
- Uses `InstrumentScreen.getInstrumentId()` to identify which instrument is being played
- Three custom packets: `SelectAuraC2SPacket` (aura selection), `InstrumentOpenC2SPacket` (instrument ID), `SyncAuraSelectionS2CPacket` (server→client default sync)
- Client overlay injected via `ScreenEvent.Init.Post` using `event.addListener()`
- All Even More Instruments screens extend GI's `InstrumentScreen`, so `instanceof` detection covers both mods automatically
- Night Vision aura uses 260-tick duration to avoid the vanilla flicker warning
- See [INSTRUMENT_AURAS.md](INSTRUMENT_AURAS.md) for the full design reference with rationale for each aura's effects and color choices

## License

MIT
