# Effective Instruments

A Minecraft Forge 1.20.1 add-on mod for [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) that adds an **aura while playing** system. Select a buff aura on any instrument screen — while you play notes, nearby allies receive beneficial potion effects.

## Features

- **Instrument-specific auras** — each instrument can have its own default aura that auto-selects when opened
- **4 built-in aura presets** — fully data-driven, add your own via JSON
- **Per-instrument memory** — manual aura overrides are remembered per-instrument within your session
- **Server-authoritative** — all effects applied server-side, no cheating
- **Automatic note detection** — hooks into Genshin Instruments' event API, no extra key presses needed
- **Fully configurable** — radius, timing, targeting rules, and per-aura overrides
- **Compatible with Even More Instruments** — works on all instrument screens from both mods out of the box

## Aura Presets

| Aura | Effects | Description |
|------|---------|-------------|
| **Soothing Hymn** | Regeneration I | Gentle healing for nearby allies |
| **Invigorating March** | Speed I + Haste I | Boost movement and mining speed |
| **Guardian Chorus** | Resistance I + Absorption I | Damage reduction and extra hearts |
| **Luminous Nocturne** | Night Vision | See clearly in the dark |

Effects are refreshed as long as you keep playing. Stop playing and they expire naturally after a few seconds.

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

**Default mappings (generated on first launch):**

| Instrument | Default Aura |
|-----------|-------------|
| Windsong Lyre | Soothing Hymn |
| Vintage Lyre | Soothing Hymn |
| Floral Zither | Guardian Chorus |
| Glorious Drum | Invigorating March |
| Nightwind Horn | Luminous Nocturne |
| Ukulele | Soothing Hymn |
| Djem Djem Drum | Invigorating March |

**Adding more auras to an instrument:** Change the string shorthand to the object form:

```json
{
  "genshinstrument:windsong_lyre": {
    "default": "soothing_hymn",
    "allowed": ["soothing_hymn", "guardian_chorus", "luminous_nocturne"]
  }
}
```

- **String form** (`"instrument": "aura"`) — single aura, default and only option
- **Object form** — `"default"` is auto-selected on open, `"allowed"` lists all auras shown in selector
- **Unmapped instruments** show all auras (backwards-compatible)

Even More Instruments screens are not mapped by default — add them manually. See `_README_INSTRUMENTS.txt` in the config folder for instrument IDs.

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

## License

MIT
