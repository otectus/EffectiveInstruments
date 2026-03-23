# Effective Instruments

**Play music. Empower allies.** Effective Instruments adds a magical aura system to [Genshin Instruments](https://www.curseforge.com/minecraft/mc-mods/genshin-instruments), letting musicians grant potion effects to nearby players and tamed pets while they perform. Pick an aura, play your instrument, and watch enchanted music notes drift outward as your allies receive buffs.

---

## How It Works

1. **Open any instrument** from Genshin Instruments (or Even More Instruments).
2. **Select an aura** from the icon buttons that appear in the top-right corner of the instrument screen.
3. **Start playing.** As long as you're actively playing notes, your aura applies its potion effects to all valid targets within range. Colored music note particles float outward to show the aura's reach.
4. **Stop playing** and the aura deactivates after a short grace window (configurable, default 5 seconds). Effects are applied with a set duration and will naturally expire on their own.

Switching to a different aura **immediately clears** the previous aura's effects from all targets before the new one kicks in — no stacking exploits, clean transitions.

---

## Built-In Auras

Effective Instruments ships with four aura presets out of the box:

| Aura | Effects | Color |
|---|---|---|
| **Soothing Hymn** | Regeneration I | Green |
| **Invigorating March** | Speed I + Haste I | Orange |
| **Guardian Chorus** | Resistance I + Absorption I | Blue |
| **Luminous Nocturne** | Night Vision | Lavender |

Each has its own unique icon in the instrument UI, with a highlighted variant when selected. All four can be freely customized, disabled, or deleted entirely.

---

## Fully Data-Driven Aura System

Every aura is defined by a simple JSON file in your config folder:

```
config/effective_instruments/auras/
```

On first launch, the mod generates the four default aura JSON files and a `_README.txt` reference guide. From there, you have full control:

- **Edit** any default aura — change its effects, duration, radius, color, name, or description
- **Disable** an aura by setting `"enabled": false` — it stays on disk but won't appear in-game
- **Delete** a default aura file — it won't be regenerated
- **Create new auras** by adding your own `.json` files to the folder

### Aura JSON Format

```json
{
  "displayName": "Fire Ward",
  "description": "Fire Resistance to nearby allies",
  "color": "FF4400",
  "enabled": true,
  "durationTicks": 200,
  "radius": 12,
  "sortOrder": 10,
  "effects": [
    { "effect": "minecraft:fire_resistance", "amplifier": 0 }
  ]
}
```

**Fields:**

| Field | Type | Description |
|---|---|---|
| `displayName` | string | Name shown on the button tooltip |
| `description` | string | Tooltip description line |
| `color` | string | Hex color code (e.g. `"FF8800"`) — used for particles and UI highlights |
| `enabled` | bool | Whether this aura appears in the selector |
| `durationTicks` | int | How long applied effects last (20 ticks = 1 second) |
| `radius` | int | Aura range in blocks. Use `-1` to inherit the global server default |
| `sortOrder` | int | Button ordering in the UI (lower = further left) |
| `effects` | array | List of potion effects to apply (see below) |
| `icon` | string | *(Optional)* Resource location for the button icon texture |
| `iconSelected` | string | *(Optional)* Resource location for the selected icon texture |

Each entry in the `effects` array takes:
- `"effect"` — the registry name of the potion effect (e.g. `"minecraft:regeneration"`, `"minecraft:speed"`)
- `"amplifier"` — effect level minus 1 (0 = Level I, 1 = Level II, etc.)

If `icon`/`iconSelected` are omitted, the button displays the first letter of the display name as a fallback — so custom auras don't require any texture work.

### Hot Reload

Use the command `/effectiveinstruments reload` (requires operator permission level 2) to reload all aura JSON files without restarting the game. If a player has an aura selected that no longer exists or was disabled after reload, their selection is automatically cleared.

---

## Configuration

All config files live under a single folder: `config/effective_instruments/`.

### Server Config (`server.toml`)

**General settings:**

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle for all aura effects |
| `noteWindowTicks` | `100` | Ticks after the last note before the aura deactivates (100 = 5 sec) |
| `auraTickIntervalTicks` | `10` | How often (in ticks) aura effects refresh on targets |
| `defaultRadius` | `16` | Default aura range in blocks (used when a preset's radius is `-1`) |

**Targeting settings:**

| Setting | Default | Description |
|---|---|---|
| `allowSelfBuff` | `true` | Whether the musician receives their own aura effects |
| `includeOtherPlayers` | `true` | Whether other players in range receive effects |
| `includeTamedPets` | `true` | Whether tamed animals in range receive effects |
| `petEntityTypeAllowlist` | `[]` | Extra entity type IDs to treat as pets (e.g. `["alexsmobs:crow"]`) |
| `screenClassAllowlist` | `[]` | Fully-qualified class names for instrument screens from other mods that don't extend Genshin Instruments' `InstrumentScreen` |

### Client Config (`client.toml`)

| Setting | Default | Description |
|---|---|---|
| `showOverlay` | `true` | Show the aura selector buttons on instrument screens |
| `overlayScale` | `1.0` | Scale factor for overlay buttons (0.5 - 2.0) |
| `compactMode` | `false` | Use a compact button layout |
| `particlesMode` | `ALL` | Floating music note particles: `ALL`, `MINIMAL`, or `NONE` |

---

## Visual Effects

When an aura is active, colored **floating music note particles** spawn across the full radius of the aura's effect range. The particles are tinted to match the aura's configured color — green drifting notes for Soothing Hymn, orange for Invigorating March, and so on. They gently rise, drift, pulse in size, and fade out.

Particle rendering is entirely client-side. If particles cause performance issues, use the `particlesMode` client config option to reduce or disable them. The server always sends particle data; the client decides whether to render it.

---

## Smart Buff Handling

- **Strongest wins:** If a target already has a stronger version of the same effect from another source (e.g. a beacon or potion), the aura won't overwrite it.
- **Clean switching:** Changing auras instantly strips the old aura's effects from all targets. The mod tracks exactly which effects it applied to which entities and only removes its own.
- **Ambient effects:** Aura-applied effects use the ambient flag (subtle swirling particles) to distinguish them from potions and to keep the visual clutter low.
- **Deselecting:** Clicking the active aura button to deselect lets existing effects expire naturally rather than stripping them immediately.

---

## Compatibility

- **Genshin Instruments** (required) — all instrument screens are automatically detected
- **Even More Instruments** (optional) — all EMI screens extend Genshin Instruments' `InstrumentScreen` and are automatically supported
- **Other instrument mods** — use the `screenClassAllowlist` server config to add support for screens from mods that don't extend `InstrumentScreen`

Effects from other mods (potion effects, beacons, etc.) are never stripped or overwritten unless the aura provides a stronger or equal version.

---

## Requirements

- **Minecraft** 1.20.1
- **Forge** 47+
- **Genshin Instruments** 5.0+

---

## Quick Start

1. Install the mod alongside Genshin Instruments.
2. Open any instrument in-game. You'll see four aura icons in the top-right corner.
3. Click one to select it, then start playing. Nearby allies will receive the buff.
4. To customize or add auras, edit the JSON files in `config/effective_instruments/auras/`.
5. Use `/effectiveinstruments reload` to apply changes without restarting.
