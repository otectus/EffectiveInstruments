# Instrument Auras — Design Reference

Each instrument has a unique, thematic aura that reflects its character, cultural origin, and sonic identity. Auras are designed to feel like a natural extension of the instrument's personality.

---

## Genshin Instruments

### Windsong Lyre
**Aura:** Zephyr's Blessing
**ID:** `zephyrs_blessing`

| Field | Value |
|-------|-------|
| Effects | Speed I |
| Color | `7FDBCA` (soft teal) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *A gentle breeze carries your melody, quickening the step of all who hear it.* |

**Rationale:** The Windsong Lyre is an instrument of the wind — light, airy, and free. Speed captures the sensation of wind at your back, urging you forward across open fields.

```json
{
  "displayName": "Zephyr's Blessing",
  "description": "A gentle breeze quickens the step of nearby allies",
  "color": "7FDBCA",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 0,
  "effects": [
    { "effect": "minecraft:speed", "amplifier": 0 }
  ]
}
```

---

### Vintage Lyre
**Aura:** Echoes of Antiquity
**ID:** `echoes_of_antiquity`

| Field | Value |
|-------|-------|
| Effects | Regeneration I |
| Color | `D4A574` (warm amber) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *Ancient melodies stir something deep within, mending wounds with the patience of centuries.* |

**Rationale:** The Vintage Lyre is weathered, old, and full of history. Regeneration evokes the slow, steady healing of time — scars fading, bones knitting, the body remembering how to be whole.

```json
{
  "displayName": "Echoes of Antiquity",
  "description": "Ancient melodies mend the wounds of nearby allies",
  "color": "D4A574",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 1,
  "effects": [
    { "effect": "minecraft:regeneration", "amplifier": 0 }
  ]
}
```

---

### Floral Zither
**Aura:** Bloom Veil
**ID:** `bloom_veil`

| Field | Value |
|-------|-------|
| Effects | Absorption I + Saturation I |
| Color | `FF88CC` (cherry blossom pink) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *Petals of sound swirl around your allies, nourishing body and shielding spirit.* |

**Rationale:** The Floral Zither evokes gardens in full bloom — abundance, nourishment, and natural beauty. Absorption provides a shield of petals while Saturation represents the bounty of the harvest.

```json
{
  "displayName": "Bloom Veil",
  "description": "Nourishes and shields nearby allies with floral energy",
  "color": "FF88CC",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 2,
  "effects": [
    { "effect": "minecraft:absorption", "amplifier": 0 },
    { "effect": "minecraft:saturation", "amplifier": 0 }
  ]
}
```

---

### Glorious Drum
**Aura:** Warcry Cadence
**ID:** `warcry_cadence`

| Field | Value |
|-------|-------|
| Effects | Strength I + Resistance I |
| Color | `FF4422` (blazing crimson) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *Each thunderous beat steels the body and emboldens the spirit — charge forward!* |

**Rationale:** The Arataki's Great and Glorious Drum is pure percussive power. War drums have rallied armies since antiquity. Strength and Resistance turn your allies into an unstoppable vanguard.

```json
{
  "displayName": "Warcry Cadence",
  "description": "Emboldens nearby allies with strength and resilience",
  "color": "FF4422",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 3,
  "effects": [
    { "effect": "minecraft:strength", "amplifier": 0 },
    { "effect": "minecraft:resistance", "amplifier": 0 }
  ]
}
```

---

### Nightwind Horn
**Aura:** Moonlit Passage
**ID:** `moonlit_passage`

| Field | Value |
|-------|-------|
| Effects | Night Vision + Slow Falling |
| Color | `6644BB` (deep violet) |
| Duration | 260 ticks |
| Radius | -1 (default) |
| Description | *The horn's call pierces the dark, and the night itself becomes your ally — see all, fear no fall.* |

**Rationale:** The Nightwind Horn is an instrument of the night sky. Its deep, sustained tones evoke moonlit cliffs and starlit descents. Night Vision reveals the darkness while Slow Falling ensures safe passage across treacherous heights.

```json
{
  "displayName": "Moonlit Passage",
  "description": "Grants night sight and safe descent to nearby allies",
  "color": "6644BB",
  "enabled": true,
  "durationTicks": 260,
  "radius": -1,
  "sortOrder": 4,
  "effects": [
    { "effect": "minecraft:night_vision", "amplifier": 0 },
    { "effect": "minecraft:slow_falling", "amplifier": 0 }
  ]
}
```

---

### Ukulele
**Aura:** Sunkissed Serenade
**ID:** `sunkissed_serenade`

| Field | Value |
|-------|-------|
| Effects | Luck I |
| Color | `FFDD44` (sunny gold) |
| Duration | 200 ticks |
| Radius | -1 (default) |
| Description | *A carefree strum under blue skies — fortune smiles on those who stop to listen.* |

**Rationale:** The ukulele radiates warmth, joy, and island ease. It's the instrument of lazy beach afternoons and serendipitous encounters. Luck perfectly captures that breezy, "everything just works out" energy.

```json
{
  "displayName": "Sunkissed Serenade",
  "description": "Fortune favors nearby allies with improved luck",
  "color": "FFDD44",
  "enabled": true,
  "durationTicks": 200,
  "radius": -1,
  "sortOrder": 5,
  "effects": [
    { "effect": "minecraft:luck", "amplifier": 0 }
  ]
}
```

---

### Djem Djem Drum
**Aura:** Rhythm of the Earth
**ID:** `rhythm_of_the_earth`

| Field | Value |
|-------|-------|
| Effects | Haste I + Jump Boost I |
| Color | `CC6633` (terracotta) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *Primal rhythms pulse up from the ground — hands move faster, legs spring higher.* |

**Rationale:** The djembe is an instrument rooted in the earth, played with bare hands, its rhythms mirroring the heartbeat of the land. Haste captures the frenetic energy of skilled drumming while Jump Boost evokes dancers leaping in celebration.

```json
{
  "displayName": "Rhythm of the Earth",
  "description": "Primal beats hasten the hands and spring the step of nearby allies",
  "color": "CC6633",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 6,
  "effects": [
    { "effect": "minecraft:haste", "amplifier": 0 },
    { "effect": "minecraft:jump_boost", "amplifier": 0 }
  ]
}
```

---

## Even More Instruments

### Guitar
**Aura:** Wanderer's Anthem
**ID:** `wanderers_anthem`

| Field | Value |
|-------|-------|
| Effects | Speed I + Jump Boost I |
| Color | `B8860B` (dark goldenrod) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *A traveler's tune for the open road — stride longer, leap higher, never stop moving.* |

**Rationale:** The acoustic guitar is the instrument of wanderers, buskers, and campfire storytellers. It belongs to the road. Speed and Jump Boost embody the restless spirit of a musician who never stays in one place for long.

```json
{
  "displayName": "Wanderer's Anthem",
  "description": "Quickens stride and lightens step for nearby allies",
  "color": "B8860B",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 7,
  "effects": [
    { "effect": "minecraft:speed", "amplifier": 0 },
    { "effect": "minecraft:jump_boost", "amplifier": 0 }
  ]
}
```

---

### Keyboard
**Aura:** Harmonic Resonance
**ID:** `harmonic_resonance`

| Field | Value |
|-------|-------|
| Effects | Regeneration I + Haste I |
| Color | `EEEEFF` (silver-white) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *Precise chords ripple outward in perfect harmony, restoring and energizing all within earshot.* |

**Rationale:** The keyboard (piano) is the most complete instrument — it contains all notes, all harmonies. Its mathematical precision and full range make it the instrument of balance. Regeneration heals while Haste energizes, covering both sides of the coin.

```json
{
  "displayName": "Harmonic Resonance",
  "description": "Precise harmonies restore and energize nearby allies",
  "color": "EEEEFF",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 8,
  "effects": [
    { "effect": "minecraft:regeneration", "amplifier": 0 },
    { "effect": "minecraft:haste", "amplifier": 0 }
  ]
}
```

---

### Koto
**Aura:** Tranquil Current
**ID:** `tranquil_current`

| Field | Value |
|-------|-------|
| Effects | Water Breathing + Dolphin's Grace |
| Color | `44AACC` (clear water blue) |
| Duration | 260 ticks |
| Radius | -1 (default) |
| Description | *Each plucked string sends ripples through the air like stones across still water — breathe deep, swim far.* |

**Rationale:** The koto's crystalline, flowing tones evoke streams cascading over smooth stones. In Japanese aesthetics, the koto is deeply connected to nature and water imagery. Water Breathing and Dolphin's Grace turn the ocean into a second home.

```json
{
  "displayName": "Tranquil Current",
  "description": "Flowing tones grant water breathing and aquatic grace to nearby allies",
  "color": "44AACC",
  "enabled": true,
  "durationTicks": 260,
  "radius": -1,
  "sortOrder": 9,
  "effects": [
    { "effect": "minecraft:water_breathing", "amplifier": 0 },
    { "effect": "minecraft:dolphins_grace", "amplifier": 0 }
  ]
}
```

---

### Pipa
**Aura:** Silk Road Vigor
**ID:** `silk_road_vigor`

| Field | Value |
|-------|-------|
| Effects | Speed I + Strength I |
| Color | `FF6644` (cinnabar red) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *Rapid-fire notes strike like hooves on the trade road — keep pace or be left behind.* |

**Rationale:** The pipa is famous for its aggressive, rapid tremolo technique — "the clash of arms on the battlefield." Chinese poetry compares its sound to cavalry charges and merchant caravans racing along the Silk Road. Speed and Strength embody that fierce momentum.

```json
{
  "displayName": "Silk Road Vigor",
  "description": "Fierce melodies drive nearby allies to move and strike with vigor",
  "color": "FF6644",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 10,
  "effects": [
    { "effect": "minecraft:speed", "amplifier": 0 },
    { "effect": "minecraft:strength", "amplifier": 0 }
  ]
}
```

---

### Saxophone
**Aura:** Smoky Allure
**ID:** `smoky_allure`

| Field | Value |
|-------|-------|
| Effects | Hero of the Village I |
| Color | `DAA520` (smoky gold) |
| Duration | 200 ticks |
| Radius | -1 (default) |
| Description | *Sultry jazz drifts through the air — even the villagers can't help but offer their best deals.* |

**Rationale:** The saxophone is the instrument of cool confidence, smoky jazz clubs, and magnetic charisma. Hero of the Village captures that effortless charm — when you play sax, everyone wants to be your friend (and give you discounts).

```json
{
  "displayName": "Smoky Allure",
  "description": "Charismatic jazz melodies charm villagers into offering better trades",
  "color": "DAA520",
  "enabled": true,
  "durationTicks": 200,
  "radius": -1,
  "sortOrder": 11,
  "effects": [
    { "effect": "minecraft:hero_of_the_village", "amplifier": 0 }
  ]
}
```

---

### Shamisen
**Aura:** Ghost Flame
**ID:** `ghost_flame`

| Field | Value |
|-------|-------|
| Effects | Fire Resistance + Strength I |
| Color | `88CCFF` (spectral ice-blue) |
| Duration | 200 ticks |
| Radius | -1 (default) |
| Description | *Sharp, haunting notes summon phantom fire that shields from flame and hardens resolve.* |

**Rationale:** The shamisen is the instrument of kabuki theatre, ghost stories, and supernatural drama. Its sharp, percussive twang evokes will-o'-wisps and spirit fire. Fire Resistance reflects mastery over flame, while Strength channels the fierce determination of a warrior-spirit.

```json
{
  "displayName": "Ghost Flame",
  "description": "Spectral fire shields from flame and emboldens nearby allies",
  "color": "88CCFF",
  "enabled": true,
  "durationTicks": 200,
  "radius": -1,
  "sortOrder": 12,
  "effects": [
    { "effect": "minecraft:fire_resistance", "amplifier": 0 },
    { "effect": "minecraft:strength", "amplifier": 0 }
  ]
}
```

---

### Trombone
**Aura:** Bulwark Fanfare
**ID:** `bulwark_fanfare`

| Field | Value |
|-------|-------|
| Effects | Resistance I + Absorption II |
| Color | `CC8800` (brass) |
| Duration | 160 ticks |
| Radius | -1 (default) |
| Description | *A triumphant brass fanfare rings out — shields raise, hearts swell, nothing gets through.* |

**Rationale:** The trombone is the backbone of brass sections, marching bands, and military fanfares. Its bold, resonant tone commands attention and inspires confidence. Resistance and enhanced Absorption create an impenetrable wall — the musical equivalent of locking shields.

```json
{
  "displayName": "Bulwark Fanfare",
  "description": "Triumphant brass fortifies and shields nearby allies",
  "color": "CC8800",
  "enabled": true,
  "durationTicks": 160,
  "radius": -1,
  "sortOrder": 13,
  "effects": [
    { "effect": "minecraft:resistance", "amplifier": 0 },
    { "effect": "minecraft:absorption", "amplifier": 1 }
  ]
}
```

---

### Violin
**Aura:** Heartstring Aria
**ID:** `heartstring_aria`

| Field | Value |
|-------|-------|
| Effects | Regeneration I + Absorption I |
| Color | `CC4466` (rosewood) |
| Duration | 200 ticks |
| Radius | -1 (default) |
| Description | *A soaring melody that pulls at the heartstrings — wounds close, spirits lift, the body endures.* |

**Rationale:** The violin is the instrument closest to the human voice. Its expressive range from tender whisper to passionate cry makes it the ultimate instrument of emotion. Regeneration heals while Absorption cushions — the musical equivalent of being held and told everything will be alright.

```json
{
  "displayName": "Heartstring Aria",
  "description": "A soaring melody that heals and shields nearby allies",
  "color": "CC4466",
  "enabled": true,
  "durationTicks": 200,
  "radius": -1,
  "sortOrder": 14,
  "effects": [
    { "effect": "minecraft:regeneration", "amplifier": 0 },
    { "effect": "minecraft:absorption", "amplifier": 0 }
  ]
}
```

---

## Quick Reference

| # | Instrument | Aura Name | Effects | Color |
|---|-----------|-----------|---------|-------|
| 1 | Windsong Lyre | Zephyr's Blessing | Speed I | `7FDBCA` |
| 2 | Vintage Lyre | Echoes of Antiquity | Regeneration I | `D4A574` |
| 3 | Floral Zither | Bloom Veil | Absorption I + Saturation I | `FF88CC` |
| 4 | Glorious Drum | Warcry Cadence | Strength I + Resistance I | `FF4422` |
| 5 | Nightwind Horn | Moonlit Passage | Night Vision + Slow Falling | `6644BB` |
| 6 | Ukulele | Sunkissed Serenade | Luck I | `FFDD44` |
| 7 | Djem Djem Drum | Rhythm of the Earth | Haste I + Jump Boost I | `CC6633` |
| 8 | Guitar | Wanderer's Anthem | Speed I + Jump Boost I | `B8860B` |
| 9 | Keyboard | Harmonic Resonance | Regeneration I + Haste I | `EEEEFF` |
| 10 | Koto | Tranquil Current | Water Breathing + Dolphin's Grace | `44AACC` |
| 11 | Pipa | Silk Road Vigor | Speed I + Strength I | `FF6644` |
| 12 | Saxophone | Smoky Allure | Hero of the Village I | `DAA520` |
| 13 | Shamisen | Ghost Flame | Fire Resistance + Strength I | `88CCFF` |
| 14 | Trombone | Bulwark Fanfare | Resistance I + Absorption II | `CC8800` |
| 15 | Violin | Heartstring Aria | Regeneration I + Absorption I | `CC4466` |

---

## Design Principles

1. **No two instruments share the exact same effect combination** — every aura feels distinct
2. **Effects match the instrument's cultural and sonic identity** — not arbitrary assignments
3. **Balanced power levels** — combat instruments (Drum, Shamisen, Pipa) get offensive effects; gentle instruments (Lyres, Violin) get healing/utility; exotic instruments (Koto, Sax) get niche effects
4. **Colors reflect the aura's mood** — warm tones for energy, cool tones for calm, deep tones for mystery
5. **Duration scales with utility** — vision/breathing effects use 260 ticks (avoid flicker), combat effects use 160 ticks (standard refresh)
