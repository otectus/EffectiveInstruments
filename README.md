# Effective Instruments

A Minecraft Forge 1.20.1 add-on mod for [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) that adds an **aura while playing** system. Select a buff aura on any instrument screen — while you play notes, nearby allies receive beneficial potion effects.

## Features

- **4 Aura Presets** — choose one while playing any instrument
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
2. Click one of the aura buttons in the top-right corner
3. Play notes — allies within 16 blocks (configurable) receive the selected buff
4. Click the active aura again to deselect, or close the instrument to stop

**Who receives buffs:**
- Yourself (toggleable)
- Other players in range (toggleable)
- Your tamed animals in range (toggleable) — wolves, cats, horses, parrots, etc.

Effects use a **strongest-wins** policy: if an ally already has a stronger version of the same effect (e.g. Resistance II from a Beacon), the aura will not downgrade it.

## Requirements

| Dependency | Version | Required? |
|------------|---------|-----------|
| Minecraft | 1.20.1 | Yes |
| Forge | 47.3.0+ | Yes |
| Genshin Instruments | 5.0+ | Yes |
| Even More Instruments | 6.0+ | No (optional) |

## Configuration

Config files are generated on first launch in the `config/` folder.

### Server Config (`effectiveinstruments-server.toml`)

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
| `screenClassAllowlist` | `[]` | Fallback class names for non-GI instrument screens |

#### Per-Aura Overrides
Each aura has its own subsection with:
| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `true` | Enable/disable this specific aura |
| `durationOverride` | `-1` | Override effect duration in ticks (-1 = use default) |
| `radiusOverride` | `-1` | Override radius in blocks (-1 = use global default) |

### Client Config (`effectiveinstruments-client.toml`)

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `showOverlay` | `true` | — | Show aura selector on instrument screens |
| `overlayScale` | `1.0` | 0.5–2.0 | Button scale factor |
| `compactMode` | `false` | — | Smaller button layout |
| `particlesMode` | `ALL` | ALL / MINIMAL / NONE | Particle effects when aura is active |

## Building from Source

Requires **Java 17**.

```bash
# If your system JAVA_HOME points elsewhere, override it:
JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew build
```

The built JAR will be at `build/libs/effectiveinstruments-1.0.0.jar`.

## Technical Details

- Subscribes to Genshin Instruments' `InstrumentPlayedEvent` for note detection — no custom note packets, no mixins, no reflection
- Subscribes to `InstrumentOpenStateChangedEvent` for instrument open/close tracking
- Single custom packet: `SelectAuraC2SPacket` (client tells server which aura is selected)
- Client overlay injected via `ScreenEvent.Init.Post` using `event.addListener()` — the same proven pattern used by Even More Instruments
- All Even More Instruments screens extend GI's `InstrumentScreen`, so `instanceof` detection covers both mods automatically
- Night Vision aura uses 260-tick duration to avoid the vanilla flicker warning (which triggers below 200 ticks remaining)

## License

MIT
