#!/usr/bin/env python3
"""
Unified aura-icon generator for Effective Instruments v1.4.2.

Replaces the older ``gen_offensive_icons.py``. Writes every aura's icon —
positive and offensive, stationary and mobile — with a glyph chosen from
its primary MobEffect so each aura has a distinct-ish look without
hand-drawing 52 individual PNGs.

Output: 104 PNG files under
    src/main/resources/assets/effectiveinstruments/textures/gui/
    aura_<id>.png           (idle)
    aura_<id>_selected.png  (selected — brighter border)

Idempotent. Run after changing any of:
    * the ``PRESETS`` table below
    * the ``GLYPHS`` bitmaps
    * the ``EFFECT_TO_GLYPH`` mapping

Requires Pillow (``pip install pillow`` or ``apt install python3-pil``).
"""

from __future__ import annotations

import pathlib
from dataclasses import dataclass
from typing import Literal

from PIL import Image

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT_DIR = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "effectiveinstruments"
    / "textures"
    / "gui"
)


# -- Preset table ------------------------------------------------------------

Polarity = Literal["positive", "offensive"]


@dataclass(frozen=True)
class Preset:
    id: str
    color_hex: str
    polarity: Polarity
    primary_effect: str  # namespaced effect id, drives glyph selection


PRESETS: list[Preset] = [
    # --- Stationary positive (15) ---
    Preset("zephyrs_blessing",    "7FDBCA", "positive",  "minecraft:speed"),
    Preset("echoes_of_antiquity", "D4A574", "positive",  "minecraft:regeneration"),
    Preset("bloom_veil",          "FF88CC", "positive",  "minecraft:absorption"),
    Preset("warcry_cadence",      "FF4422", "positive",  "minecraft:strength"),
    Preset("moonlit_passage",     "6644BB", "positive",  "minecraft:night_vision"),
    Preset("sunkissed_serenade",  "FFDD44", "positive",  "minecraft:luck"),
    Preset("rhythm_of_the_earth", "CC6633", "positive",  "minecraft:haste"),
    Preset("wanderers_anthem",    "B8860B", "positive",  "minecraft:speed"),
    Preset("harmonic_resonance",  "EEEEFF", "positive",  "minecraft:regeneration"),
    Preset("tranquil_current",    "44AACC", "positive",  "minecraft:water_breathing"),
    Preset("silk_road_vigor",     "FF6644", "positive",  "minecraft:strength"),
    Preset("smoky_allure",        "DAA520", "positive",  "minecraft:hero_of_the_village"),
    Preset("ghost_flame",         "88CCFF", "positive",  "minecraft:fire_resistance"),
    Preset("bulwark_fanfare",     "CC8800", "positive",  "minecraft:resistance"),
    Preset("heartstring_aria",    "CC4466", "positive",  "minecraft:regeneration"),
    # --- Stationary offensive (15) ---
    Preset("zephyrs_wrath",       "2E4A5E", "offensive", "minecraft:slowness"),
    Preset("echoes_of_decay",     "4A3A4A", "offensive", "minecraft:wither"),
    Preset("withering_bloom",     "6B2E6B", "offensive", "minecraft:poison"),
    Preset("battle_fanfare",      "7A1F1F", "offensive", "minecraft:weakness"),
    Preset("ebon_dirge",          "2B1F4A", "offensive", "minecraft:blindness"),
    Preset("mischief_melody",     "7A6A1A", "offensive", "minecraft:blindness"),
    Preset("earth_tremor",        "5A3A1F", "offensive", "minecraft:mining_fatigue"),
    Preset("storm_drifter",       "3A2A5A", "offensive", "minecraft:weakness"),
    Preset("discordant_chord",    "3A3A4A", "offensive", "minecraft:hunger"),
    Preset("fathomless_pull",     "1F3A5A", "offensive", "minecraft:mining_fatigue"),
    Preset("winds_of_war",        "5A1F1F", "offensive", "minecraft:wither"),
    Preset("bleeding_note",       "5A4A1F", "offensive", "minecraft:levitation"),
    Preset("spectral_wail",       "3A4A4A", "offensive", "minecraft:nausea"),
    Preset("obsidian_blast",      "1A1A1A", "offensive", "minecraft:levitation"),
    Preset("dirge_of_grief",      "4A2A3A", "offensive", "minecraft:wither"),
    # --- Mobile positive (11) ---
    Preset("windstep_mobile",         "7FDBCA", "positive", "minecraft:speed"),
    Preset("traveler_hum_mobile",     "D4A574", "positive", "minecraft:luck"),
    Preset("measured_tempo_mobile",   "EEEEFF", "positive", "minecraft:haste"),
    Preset("hearthsong_mobile",       "CC4466", "positive", "minecraft:regeneration"),
    Preset("earthpulse_mobile",       "CC6633", "positive", "minecraft:jump_boost"),
    Preset("steadfast_drone_mobile",  "CC8800", "positive", "minecraft:resistance"),
    Preset("brass_call_mobile",       "FF6644", "positive", "minecraft:strength"),
    Preset("march_tap_mobile",        "B8860B", "positive", "minecraft:speed"),
    Preset("clear_ping_mobile",       "6644BB", "positive", "minecraft:night_vision"),
    Preset("stillwater_mobile",       "44AACC", "positive", "minecraft:water_breathing"),
    Preset("shade_resonance_mobile",  "88CCFF", "positive", "minecraft:fire_resistance"),
    # --- User-custom positives (shipped so instance defaults from older
    # releases still get proper icons instead of letter fallback) ---
    Preset("soothing_hymn",       "88FF88", "positive", "minecraft:regeneration"),
    Preset("guardian_chorus",     "4488FF", "positive", "minecraft:resistance"),
    Preset("invigorating_march",  "FFAA00", "positive", "minecraft:speed"),
    Preset("luminous_nocturne",   "AAAAFF", "positive", "minecraft:night_vision"),
    # --- 1.4.4: 16 new stationary positives (one per EMI note-block variant) ---
    Preset("skyward_zephyr",      "B4D8FF", "positive", "minecraft:speed"),
    Preset("rumbling_anthem",     "8B4513", "positive", "minecraft:strength"),
    Preset("thunderous_cadence",  "CC3300", "positive", "minecraft:strength"),
    Preset("drumline_vigor",      "D2B48C", "positive", "minecraft:speed"),
    Preset("artisan_tempo",       "E0E0E0", "positive", "minecraft:haste"),
    Preset("chiming_revival",     "FFE066", "positive", "minecraft:regeneration"),
    Preset("starlit_grace",       "AACCFF", "positive", "minecraft:night_vision"),
    Preset("fleetfoot_lilt",      "9FE5B3", "positive", "minecraft:speed"),
    Preset("troubadour_march",    "5A3A6E", "positive", "minecraft:strength"),
    Preset("craftwork_rondo",     "F0D67A", "positive", "minecraft:haste"),
    Preset("ironwright_anthem",   "B0B8C0", "positive", "minecraft:resistance"),
    Preset("pasture_serenade",    "D9A43C", "positive", "minecraft:regeneration"),
    Preset("hearthlight_drone",   "FF8844", "positive", "minecraft:fire_resistance"),
    Preset("pixel_pulse",         "66DDAA", "positive", "minecraft:haste"),
    Preset("wayfinders_reel",     "CC9933", "positive", "minecraft:speed"),
    Preset("bellwether_toll",     "8F8FFF", "positive", "minecraft:night_vision"),
    # --- 1.4.4: 16 new stationary offensives (mirrors the positives above) ---
    Preset("skyward_blight",      "3A4A6E", "offensive", "minecraft:slowness"),
    Preset("rumbling_curse",      "3A2A1F", "offensive", "minecraft:weakness"),
    Preset("thunderous_dirge",    "4A2A2A", "offensive", "minecraft:weakness"),
    Preset("drumline_blight",     "5A2A4A", "offensive", "minecraft:slowness"),
    Preset("artisan_curse",       "3A3A1F", "offensive", "minecraft:mining_fatigue"),
    Preset("tolling_entropy",     "4A3A2A", "offensive", "minecraft:wither"),
    Preset("starlit_malice",      "2A2A4A", "offensive", "minecraft:blindness"),
    Preset("fleetfoot_fall",      "3A3A5A", "offensive", "minecraft:levitation"),
    Preset("troubadour_dirge",    "4A2A4A", "offensive", "minecraft:blindness"),
    Preset("craftwork_rot",       "3A3A2A", "offensive", "minecraft:hunger"),
    Preset("ironwright_curse",    "2A2A2A", "offensive", "minecraft:mining_fatigue"),
    Preset("pasture_rot",         "4A3A1F", "offensive", "minecraft:wither"),
    Preset("hearthshade_dirge",   "2A1A1A", "offensive", "minecraft:wither"),
    Preset("pixel_rot",           "3A3A3A", "offensive", "minecraft:slowness"),
    Preset("wayfinders_lament",   "4A3A1A", "offensive", "minecraft:slowness"),
    Preset("bellwether_rot",      "3A2A3A", "offensive", "minecraft:mining_fatigue"),
    # --- Mobile offensive (11) ---
    Preset("gale_shock_mobile",         "2E4A5E", "offensive", "minecraft:slowness"),
    Preset("calamitys_chant_mobile",    "4A3A4A", "offensive", "minecraft:poison"),
    Preset("iron_hammer_mobile",        "3A3A4A", "offensive", "minecraft:weakness"),
    Preset("winters_chill_mobile",      "1F3A5A", "offensive", "minecraft:slowness"),
    Preset("stone_ward_mobile",         "5A3A1F", "offensive", "minecraft:mining_fatigue"),
    Preset("battle_march_mobile",       "7A1F1F", "offensive", "minecraft:weakness"),
    Preset("war_cry_mobile",            "5A1F1F", "offensive", "minecraft:weakness"),
    Preset("war_drum_mobile",           "7A6A1A", "offensive", "minecraft:slowness"),
    Preset("echoing_chill_mobile",      "2B1F4A", "offensive", "minecraft:blindness"),
    Preset("abyssal_rattle_mobile",     "3A4A4A", "offensive", "minecraft:nausea"),
    Preset("soul_tremor_mobile",        "1A1A2A", "offensive", "minecraft:wither"),
]


# -- Glyphs -----------------------------------------------------------------
#
# Each glyph is a list of strings, drawn with 'X' for opaque and '.' for
# transparent. All glyphs must have the same dimensions — 10 columns × 10 rows
# so they fit neatly inside a 16×16 canvas with a 1px border and 2px padding.

GLYPH_H = 10
GLYPH_W = 10
GLYPH_OFFSET = ((16 - GLYPH_W) // 2, (16 - GLYPH_H) // 2)


GLYPHS: dict[str, list[str]] = {
    # --- Positive glyphs ---
    "arrow_up": [  # speed, jump boost, haste, luck, hero
        "....XX....",
        "...XXXX...",
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXX.XX.XXX",
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
    ],
    "heart": [  # regeneration, saturation
        ".XXX..XXX.",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
        "...XXXX...",
        "....XX....",
        "..........",
    ],
    "shield": [  # resistance, absorption
        "XXXXXXXXXX",
        "X........X",
        "X..XXXX..X",
        "X..XXXX..X",
        "X.XXXXXX.X",
        "X.XXXXXX.X",
        "X..XXXX..X",
        ".X.XXXX.X.",
        "..X.XX.X..",
        "...XXXX...",
    ],
    "eye": [  # night vision
        "..........",
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXXX..XXXX",
        "XXX.XX.XXX",
        "XXX.XX.XXX",
        "XXXX..XXXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
        "..........",
    ],
    "wave": [  # water breathing
        "..........",
        "..........",
        "..XX......",
        ".XXXX..XX.",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".XX..XXXX.",
        "......XX..",
        "..........",
        "..........",
    ],
    "flame": [  # fire resistance
        "....XX....",
        "....XX....",
        "...XXXX...",
        "...XXXX...",
        "..XXXXXX..",
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXX.XX.XXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
    ],
    # --- Offensive glyphs ---
    "skull": [  # poison, wither
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXXXXXXXXX",
        "XX.XX.XX.X",
        "XX.XX.XX.X",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".X.XX.XX.X",
        ".X.XX.XX.X",
        "..X..X..X.",
    ],
    "arrow_down": [  # slowness
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
        "XXX.XX.XXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
        "...XXXX...",
        "....XX....",
    ],
    "broken_sword": [  # weakness, mining fatigue
        "....XX....",
        "...XXXX...",
        "...XXXX...",
        "...XXXX...",
        "....XX....",
        "..X....X..",
        ".XX....XX.",
        "XXX.XXXXXX",
        "......XXX.",
        ".......XX.",
    ],
    "empty_bowl": [  # hunger
        "..........",
        "..........",
        "..........",
        "..........",
        "..XXXXXX..",
        "XXXXXXXXXX",
        "XX......XX",
        ".XX....XX.",
        "..XXXXXX..",
        "..........",
    ],
    "closed_eye": [  # blindness
        "..........",
        "..........",
        ".XXXXXXXX.",
        "XXXXXXXXXX",
        "X..X..X..X",
        "..........",
        "..........",
        "..........",
        "..........",
        "..........",
    ],
    "spiral": [  # nausea
        "..XXXXXX..",
        ".X......X.",
        "X..XXXX..X",
        "X.X....X.X",
        "X.X.XX.X.X",
        "X.X..X.X.X",
        "X..XXX.X.X",
        "X......X.X",
        ".XXXXXXXX.",
        "..........",
    ],
    "up_x": [  # levitation
        "....XX....",
        "...XXXX...",
        "..XXXXXX..",
        ".XX.XX.XX.",
        "XX..XX..XX",
        "..X.XX.X..",
        "...XXXX...",
        "..XX..XX..",
        ".XX....XX.",
        "XX......XX",
    ],
}


EFFECT_TO_GLYPH: dict[str, str] = {
    # Positive
    "minecraft:speed":                "arrow_up",
    "minecraft:jump_boost":           "arrow_up",
    "minecraft:haste":                "arrow_up",
    "minecraft:luck":                 "arrow_up",
    "minecraft:hero_of_the_village":  "arrow_up",
    "minecraft:strength":             "arrow_up",
    "minecraft:regeneration":         "heart",
    "minecraft:saturation":           "heart",
    "minecraft:resistance":           "shield",
    "minecraft:absorption":           "shield",
    "minecraft:night_vision":         "eye",
    "minecraft:water_breathing":      "wave",
    "minecraft:dolphins_grace":       "wave",
    "minecraft:fire_resistance":      "flame",
    "minecraft:slow_falling":         "heart",  # reused — "gentle" family
    # Offensive
    "minecraft:poison":               "skull",
    "minecraft:wither":               "skull",
    "minecraft:slowness":             "arrow_down",
    "minecraft:weakness":             "broken_sword",
    "minecraft:mining_fatigue":       "broken_sword",
    "minecraft:blindness":            "closed_eye",
    "minecraft:nausea":               "spiral",
    "minecraft:hunger":               "empty_bowl",
    "minecraft:levitation":           "up_x",
}


# -- Drawing ----------------------------------------------------------------

def hex_to_rgb(h: str) -> tuple[int, int, int]:
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def darker(rgb: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(max(0, int(c * (1.0 - amount))) for c in rgb)  # type: ignore[return-value]


def lighter(rgb: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(min(255, int(c + (255 - c) * amount)) for c in rgb)  # type: ignore[return-value]


def draw_icon(preset: Preset, selected: bool) -> Image.Image:
    base = hex_to_rgb(preset.color_hex)
    fill = base if selected else darker(base, 0.15)

    if preset.polarity == "positive":
        border = (120, 220, 120, 255) if selected else (40, 120, 40, 255)
    else:
        border = (255, 80, 80, 255) if selected else (150, 30, 30, 255)

    glyph_name = EFFECT_TO_GLYPH.get(preset.primary_effect, "arrow_up")
    glyph_rows = GLYPHS[glyph_name]
    glyph_rgb = (255, 255, 255) if selected else lighter(base, 0.7)

    img = Image.new("RGBA", (16, 16), (*fill, 255))

    # Border
    for i in range(16):
        img.putpixel((i, 0), border)
        img.putpixel((i, 15), border)
        img.putpixel((0, i), border)
        img.putpixel((15, i), border)

    # Glyph
    gx0, gy0 = GLYPH_OFFSET
    for gy, row in enumerate(glyph_rows):
        for gx, ch in enumerate(row):
            if ch != "X":
                continue
            x = gx0 + gx
            y = gy0 + gy
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (*glyph_rgb, 255))

    return img


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    seen_ids: set[str] = set()
    written = 0
    for preset in PRESETS:
        if preset.id in seen_ids:
            raise SystemExit(f"duplicate preset id: {preset.id}")
        seen_ids.add(preset.id)
        draw_icon(preset, selected=False).save(OUT_DIR / f"aura_{preset.id}.png")
        draw_icon(preset, selected=True).save(OUT_DIR / f"aura_{preset.id}_selected.png")
        written += 2
    print(f"wrote {written} icon files for {len(seen_ids)} auras to {OUT_DIR}")


if __name__ == "__main__":
    main()
