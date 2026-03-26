#!/usr/bin/env python3
"""Generate 16x16 pixel art aura icons for Effective Instruments mod."""

from PIL import Image
import os

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "assets", "effectiveinstruments", "textures", "gui"
)


def hex_to_rgba(hex_str, alpha=255):
    r = int(hex_str[0:2], 16)
    g = int(hex_str[2:4], 16)
    b = int(hex_str[4:6], 16)
    return (r, g, b, alpha)


def brighten(color, factor=1.4):
    """Brighten a color for the selected variant."""
    r, g, b, a = color
    r = min(255, int(r * factor))
    g = min(255, int(g * factor))
    b = min(255, int(b * factor))
    return (r, g, b, 255)


def darken(color, factor=0.6):
    r, g, b, a = color
    return (int(r * factor), int(g * factor), int(b * factor), a)


T = (0, 0, 0, 0)  # transparent


def make_icon(pixels_func, base_color):
    """Create normal and selected icon images from a pixel function."""
    dark = darken(base_color, 0.65)

    # Normal variant
    img = Image.new("RGBA", (16, 16), T)
    pixels_func(img, base_color, dark)

    # Selected variant (brighter)
    sel = Image.new("RGBA", (16, 16), T)
    sel_color = brighten(base_color, 1.4)
    sel_dark = darken(sel_color, 0.65)
    pixels_func(sel, sel_color, sel_dark)

    return img, sel


def set_pixels(img, coords, color):
    """Set multiple pixels at once."""
    for x, y in coords:
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), color)


# ============================================================
# Icon drawing functions — each draws a 16x16 symbol
# ============================================================

def draw_zephyrs_blessing(img, color, dark):
    """Wind swirl — three curved sweep lines."""
    # Top swoosh
    set_pixels(img, [(5,3),(6,3),(7,3),(8,3),(9,3),(10,3)], color)
    set_pixels(img, [(4,4),(11,4)], color)
    set_pixels(img, [(11,2)], dark)

    # Middle swoosh
    set_pixels(img, [(3,7),(4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7)], color)
    set_pixels(img, [(12,6)], dark)
    set_pixels(img, [(2,8)], dark)

    # Bottom swoosh
    set_pixels(img, [(5,11),(6,11),(7,11),(8,11),(9,11),(10,11)], color)
    set_pixels(img, [(4,12),(11,10)], color)
    set_pixels(img, [(4,10)], dark)

    # Wind dots
    set_pixels(img, [(13,5),(12,9),(14,7)], dark)


def draw_echoes_of_antiquity(img, color, dark):
    """Hourglass shape."""
    # Top bar
    set_pixels(img, [(4,2),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,2)], color)
    # Bottom bar
    set_pixels(img, [(4,13),(5,13),(6,13),(7,13),(8,13),(9,13),(10,13),(11,13)], color)

    # Top triangle narrowing
    set_pixels(img, [(5,3),(10,3)], color)
    set_pixels(img, [(6,4),(9,4)], color)
    set_pixels(img, [(7,5),(8,5)], color)

    # Neck
    set_pixels(img, [(7,6),(8,6)], dark)
    set_pixels(img, [(7,7),(8,7)], color)
    set_pixels(img, [(7,8),(8,8)], dark)
    set_pixels(img, [(7,9),(8,9)], dark)

    # Bottom triangle widening
    set_pixels(img, [(7,10),(8,10)], color)
    set_pixels(img, [(6,11),(9,11)], color)
    set_pixels(img, [(5,12),(10,12)], color)

    # Sand particles
    set_pixels(img, [(7,9),(8,9)], dark)


def draw_bloom_veil(img, color, dark):
    """Five-petal flower."""
    # Center
    set_pixels(img, [(7,7),(8,7),(7,8),(8,8)], brighten(color, 1.2))

    # Top petal
    set_pixels(img, [(7,3),(8,3),(7,4),(8,4),(7,5),(8,5)], color)
    set_pixels(img, [(7,2),(8,2)], dark)

    # Bottom petal
    set_pixels(img, [(7,10),(8,10),(7,11),(8,11)], color)
    set_pixels(img, [(7,12),(8,12)], dark)

    # Left petal
    set_pixels(img, [(3,7),(4,7),(5,7),(3,8),(4,8),(5,8)], color)
    set_pixels(img, [(2,7),(2,8)], dark)

    # Right petal
    set_pixels(img, [(10,7),(11,7),(12,7),(10,8),(11,8),(12,8)], color)
    set_pixels(img, [(13,7),(13,8)], dark)

    # Diagonal petals (smaller accents)
    set_pixels(img, [(5,5),(4,4)], dark)
    set_pixels(img, [(10,5),(11,4)], dark)
    set_pixels(img, [(5,10),(4,11)], dark)
    set_pixels(img, [(10,10),(11,11)], dark)


def draw_warcry_cadence(img, color, dark):
    """Crossed swords."""
    # Sword 1: top-left to bottom-right
    set_pixels(img, [(3,2),(4,3),(5,4),(6,5),(7,6),(8,7),(9,8),(10,9),(11,10),(12,11),(13,12)], color)
    # Sword 2: top-right to bottom-left
    set_pixels(img, [(12,2),(11,3),(10,4),(9,5),(8,7),(7,8),(6,9),(5,10),(4,11),(3,12)], color)

    # Hilts on sword 1
    set_pixels(img, [(2,3),(2,4)], dark)
    set_pixels(img, [(13,11),(14,11)], dark)

    # Hilts on sword 2
    set_pixels(img, [(13,3),(13,4)], dark)
    set_pixels(img, [(2,11),(2,12)], dark)

    # Cross guards
    set_pixels(img, [(4,2),(3,3)], dark)
    set_pixels(img, [(11,2),(12,3)], dark)

    # Center highlight
    set_pixels(img, [(8,8),(7,7)], brighten(color, 1.3))


def draw_moonlit_passage(img, color, dark):
    """Crescent moon with stars."""
    # Crescent moon - outer arc
    set_pixels(img, [(7,2),(8,2),(9,2),(10,3),(11,4),(11,5),(11,6),(11,7),
                      (11,8),(11,9),(10,10),(9,11),(8,11),(7,11)], color)
    # Inner cutout shadow (makes crescent shape)
    set_pixels(img, [(6,3),(7,3),(8,3),(9,4),(10,5),(10,6),(10,7),(10,8),
                      (9,9),(8,10),(7,10),(6,10)], dark)

    # Stars
    set_pixels(img, [(4,4)], brighten(color, 1.5))
    set_pixels(img, [(3,7)], brighten(color, 1.3))
    set_pixels(img, [(5,9)], brighten(color, 1.5))
    set_pixels(img, [(4,6)], color)


def draw_sunkissed_serenade(img, color, dark):
    """Sun with rays."""
    # Sun center
    set_pixels(img, [(7,6),(8,6),(6,7),(7,7),(8,7),(9,7),(6,8),(7,8),(8,8),(9,8),(7,9),(8,9)], color)
    set_pixels(img, [(7,7),(8,7),(7,8),(8,8)], brighten(color, 1.2))

    # Rays — cardinal
    set_pixels(img, [(7,3),(8,3),(7,4),(8,4)], color)  # top
    set_pixels(img, [(7,11),(8,11),(7,12),(8,12)], color)  # bottom
    set_pixels(img, [(3,7),(3,8),(4,7),(4,8)], color)  # left
    set_pixels(img, [(11,7),(11,8),(12,7),(12,8)], color)  # right

    # Rays — diagonal
    set_pixels(img, [(4,4),(5,5)], dark)
    set_pixels(img, [(11,4),(10,5)], dark)
    set_pixels(img, [(4,11),(5,10)], dark)
    set_pixels(img, [(11,11),(10,10)], dark)

    # Ray tips
    set_pixels(img, [(7,2),(8,2)], dark)
    set_pixels(img, [(7,13),(8,13)], dark)
    set_pixels(img, [(2,7),(2,8)], dark)
    set_pixels(img, [(13,7),(13,8)], dark)


def draw_rhythm_of_the_earth(img, color, dark):
    """Drum — front view with drumsticks."""
    # Drum body
    set_pixels(img, [(5,5),(6,5),(7,5),(8,5),(9,5),(10,5)], color)  # top rim
    for y in range(6, 12):
        set_pixels(img, [(4,y),(11,y)], color)  # sides
    set_pixels(img, [(5,12),(6,12),(7,12),(8,12),(9,12),(10,12)], color)  # bottom rim

    # Drum face
    for y in range(6, 12):
        set_pixels(img, [(5,y),(6,y),(7,y),(8,y),(9,y),(10,y)], dark)

    # Drum head (top) - lighter
    set_pixels(img, [(5,5),(6,5),(7,5),(8,5),(9,5),(10,5)], brighten(color, 1.1))
    set_pixels(img, [(5,6),(6,6),(7,6),(8,6),(9,6),(10,6)], color)

    # Drumstick 1 (coming from top-left)
    set_pixels(img, [(3,2),(4,3),(5,4),(6,5)], color)
    set_pixels(img, [(2,1)], dark)

    # Drumstick 2 (coming from top-right)
    set_pixels(img, [(12,2),(11,3),(10,4),(9,5)], color)
    set_pixels(img, [(13,1)], dark)

    # Decorative X pattern on drum face
    set_pixels(img, [(6,8),(9,8),(7,9),(8,9),(6,10),(9,10)], color)


def draw_wanderers_anthem(img, color, dark):
    """Compass rose."""
    # Center dot
    set_pixels(img, [(7,7),(8,7),(7,8),(8,8)], brighten(color, 1.2))

    # North arrow
    set_pixels(img, [(7,3),(8,3),(7,4),(8,4),(7,5),(8,5),(7,6),(8,6)], color)
    set_pixels(img, [(7,2),(8,2)], brighten(color, 1.3))

    # South arrow
    set_pixels(img, [(7,9),(8,9),(7,10),(8,10),(7,11),(8,11)], dark)
    set_pixels(img, [(7,12),(8,12)], dark)

    # East arrow
    set_pixels(img, [(9,7),(10,7),(11,7),(9,8),(10,8),(11,8)], color)
    set_pixels(img, [(12,7),(12,8)], dark)

    # West arrow
    set_pixels(img, [(4,7),(5,7),(6,7),(4,8),(5,8),(6,8)], dark)
    set_pixels(img, [(3,7),(3,8)], dark)

    # Diagonal ticks
    set_pixels(img, [(5,5),(10,5),(5,10),(10,10)], color)
    set_pixels(img, [(4,4),(11,4),(4,11),(11,11)], dark)


def draw_harmonic_resonance(img, color, dark):
    """Eighth note (music note)."""
    # Note head (filled oval)
    set_pixels(img, [(5,10),(6,10),(7,10),(5,11),(6,11),(7,11),(5,12),(6,12),(7,12)], color)
    set_pixels(img, [(6,11)], brighten(color, 1.2))

    # Stem
    set_pixels(img, [(8,3),(8,4),(8,5),(8,6),(8,7),(8,8),(8,9),(8,10)], color)

    # Flag
    set_pixels(img, [(9,3),(10,4),(11,5),(10,6),(9,7)], color)
    set_pixels(img, [(9,2),(10,3),(11,4)], dark)


def draw_tranquil_current(img, color, dark):
    """Water droplet with ripples."""
    # Droplet top (pointed)
    set_pixels(img, [(7,2),(8,2)], dark)
    set_pixels(img, [(7,3),(8,3)], color)
    set_pixels(img, [(6,4),(7,4),(8,4),(9,4)], color)

    # Droplet body
    set_pixels(img, [(5,5),(6,5),(7,5),(8,5),(9,5),(10,5)], color)
    set_pixels(img, [(5,6),(6,6),(7,6),(8,6),(9,6),(10,6)], color)
    set_pixels(img, [(5,7),(6,7),(7,7),(8,7),(9,7),(10,7)], color)
    set_pixels(img, [(6,8),(7,8),(8,8),(9,8)], color)

    # Highlight
    set_pixels(img, [(7,5),(7,6)], brighten(color, 1.3))

    # Ripples below
    set_pixels(img, [(4,10),(5,10),(6,10),(7,10),(8,10),(9,10),(10,10),(11,10)], dark)
    set_pixels(img, [(3,12),(4,12),(5,12),(10,12),(11,12),(12,12)], dark)


def draw_silk_road_vigor(img, color, dark):
    """Lightning bolt."""
    # Top section
    set_pixels(img, [(8,1),(9,1),(10,1)], color)
    set_pixels(img, [(7,2),(8,2),(9,2)], color)
    set_pixels(img, [(6,3),(7,3),(8,3)], color)
    set_pixels(img, [(5,4),(6,4),(7,4)], color)
    set_pixels(img, [(4,5),(5,5),(6,5)], color)

    # Middle zag
    set_pixels(img, [(5,6),(6,6),(7,6),(8,6),(9,6),(10,6)], brighten(color, 1.2))
    set_pixels(img, [(6,7),(7,7),(8,7),(9,7),(10,7)], color)

    # Bottom section
    set_pixels(img, [(9,8),(10,8),(11,8)], color)
    set_pixels(img, [(8,9),(9,9),(10,9)], color)
    set_pixels(img, [(7,10),(8,10),(9,10)], color)
    set_pixels(img, [(6,11),(7,11),(8,11)], color)
    set_pixels(img, [(5,12),(6,12),(7,12)], color)
    set_pixels(img, [(5,13),(6,13)], dark)

    # Edge highlights
    set_pixels(img, [(10,1),(10,6)], brighten(color, 1.3))


def draw_smoky_allure(img, color, dark):
    """Five-pointed star."""
    # Top point
    set_pixels(img, [(7,1),(8,1)], color)
    set_pixels(img, [(7,2),(8,2)], color)
    set_pixels(img, [(7,3),(8,3)], brighten(color, 1.2))

    # Upper body
    set_pixels(img, [(6,4),(7,4),(8,4),(9,4)], color)
    set_pixels(img, [(5,5),(6,5),(7,5),(8,5),(9,5),(10,5)], color)

    # Middle bar (widest)
    set_pixels(img, [(1,6),(2,6),(3,6),(4,6),(5,6),(6,6),(7,6),(8,6),(9,6),(10,6),(11,6),(12,6),(13,6),(14,6)], color)
    set_pixels(img, [(2,7),(3,7),(4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7),(12,7),(13,7)], color)

    # Lower body splits into two legs
    set_pixels(img, [(4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8)], color)
    set_pixels(img, [(4,9),(5,9),(6,9),(9,9),(10,9),(11,9)], color)
    set_pixels(img, [(3,10),(4,10),(5,10),(10,10),(11,10),(12,10)], color)
    set_pixels(img, [(3,11),(4,11),(11,11),(12,11)], color)
    set_pixels(img, [(2,12),(3,12),(12,12),(13,12)], dark)
    set_pixels(img, [(2,13),(3,13),(12,13),(13,13)], dark)

    # Center highlight
    set_pixels(img, [(7,6),(8,6),(7,7),(8,7)], brighten(color, 1.3))


def draw_ghost_flame(img, color, dark):
    """Spectral flame / wisp."""
    # Flame tip
    set_pixels(img, [(7,1),(8,1)], dark)
    set_pixels(img, [(7,2),(8,2)], color)

    # Upper flame
    set_pixels(img, [(6,3),(7,3),(8,3),(9,3)], color)
    set_pixels(img, [(6,4),(7,4),(8,4),(9,4)], color)
    set_pixels(img, [(5,5),(6,5),(7,5),(8,5),(9,5),(10,5)], color)

    # Middle flame body
    set_pixels(img, [(5,6),(6,6),(7,6),(8,6),(9,6),(10,6)], color)
    set_pixels(img, [(4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7)], color)
    set_pixels(img, [(4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8)], color)

    # Lower flame (wispy tendrils)
    set_pixels(img, [(4,9),(5,9),(6,9),(7,9),(8,9),(9,9),(10,9),(11,9)], color)
    set_pixels(img, [(5,10),(6,10),(7,10),(8,10),(9,10),(10,10)], dark)
    set_pixels(img, [(5,11),(6,11),(9,11),(10,11)], dark)
    set_pixels(img, [(5,12),(9,12)], dark)

    # Inner glow
    set_pixels(img, [(7,5),(8,5),(7,6),(8,6),(7,7),(8,7)], brighten(color, 1.4))

    # Ghost eyes
    set_pixels(img, [(6,7),(9,7)], (255, 255, 255, 200))


def draw_bulwark_fanfare(img, color, dark):
    """Shield shape."""
    # Top edge
    set_pixels(img, [(5,2),(6,2),(7,2),(8,2),(9,2),(10,2)], color)

    # Upper shield body
    for y in range(3, 7):
        set_pixels(img, [(4,y),(5,y),(6,y),(7,y),(8,y),(9,y),(10,y),(11,y)], color)

    # Middle narrowing
    set_pixels(img, [(5,7),(6,7),(7,7),(8,7),(9,7),(10,7)], color)
    set_pixels(img, [(5,8),(6,8),(7,8),(8,8),(9,8),(10,8)], color)
    set_pixels(img, [(6,9),(7,9),(8,9),(9,9)], color)
    set_pixels(img, [(6,10),(7,10),(8,10),(9,10)], color)
    set_pixels(img, [(7,11),(8,11)], color)
    set_pixels(img, [(7,12),(8,12)], dark)

    # Shield emblem (vertical + horizontal bars)
    set_pixels(img, [(7,4),(8,4),(7,5),(8,5),(7,6),(8,6),(7,7),(8,7),(7,8),(8,8)], brighten(color, 1.3))
    set_pixels(img, [(5,5),(6,5),(9,5),(10,5)], brighten(color, 1.3))
    set_pixels(img, [(5,6),(6,6),(9,6),(10,6)], brighten(color, 1.3))

    # Top rim highlight
    set_pixels(img, [(5,2),(6,2),(7,2),(8,2),(9,2),(10,2)], brighten(color, 1.2))


def draw_heartstring_aria(img, color, dark):
    """Heart shape."""
    # Top bumps
    set_pixels(img, [(4,4),(5,4),(6,4),(9,4),(10,4),(11,4)], color)
    set_pixels(img, [(3,5),(4,5),(5,5),(6,5),(7,5),(8,5),(9,5),(10,5),(11,5),(12,5)], color)
    set_pixels(img, [(3,6),(4,6),(5,6),(6,6),(7,6),(8,6),(9,6),(10,6),(11,6),(12,6)], color)

    # Middle body
    set_pixels(img, [(4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7)], color)
    set_pixels(img, [(5,8),(6,8),(7,8),(8,8),(9,8),(10,8)], color)
    set_pixels(img, [(6,9),(7,9),(8,9),(9,9)], color)
    set_pixels(img, [(7,10),(8,10)], color)

    # Bottom tip
    set_pixels(img, [(7,11),(8,11)], dark)

    # Highlight (shine on upper left)
    set_pixels(img, [(5,5),(5,6)], brighten(color, 1.4))


# ============================================================
# Main — generate all icons
# ============================================================

AURAS = [
    ("zephyrs_blessing",     "7FDBCA", draw_zephyrs_blessing),
    ("echoes_of_antiquity",  "D4A574", draw_echoes_of_antiquity),
    ("bloom_veil",           "FF88CC", draw_bloom_veil),
    ("warcry_cadence",       "FF4422", draw_warcry_cadence),
    ("moonlit_passage",      "6644BB", draw_moonlit_passage),
    ("sunkissed_serenade",   "FFDD44", draw_sunkissed_serenade),
    ("rhythm_of_the_earth",  "CC6633", draw_rhythm_of_the_earth),
    ("wanderers_anthem",     "B8860B", draw_wanderers_anthem),
    ("harmonic_resonance",   "EEEEFF", draw_harmonic_resonance),
    ("tranquil_current",     "44AACC", draw_tranquil_current),
    ("silk_road_vigor",      "FF6644", draw_silk_road_vigor),
    ("smoky_allure",         "DAA520", draw_smoky_allure),
    ("ghost_flame",          "88CCFF", draw_ghost_flame),
    ("bulwark_fanfare",      "CC8800", draw_bulwark_fanfare),
    ("heartstring_aria",     "CC4466", draw_heartstring_aria),
]


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    for aura_id, hex_color, draw_func in AURAS:
        base_color = hex_to_rgba(hex_color, 230)
        normal, selected = make_icon(draw_func, base_color)

        normal_path = os.path.join(OUTPUT_DIR, f"aura_{aura_id}.png")
        selected_path = os.path.join(OUTPUT_DIR, f"aura_{aura_id}_selected.png")

        normal.save(normal_path)
        selected.save(selected_path)
        print(f"  {aura_id}: {normal_path}")

    print(f"\nGenerated {len(AURAS) * 2} icon files.")


if __name__ == "__main__":
    main()
