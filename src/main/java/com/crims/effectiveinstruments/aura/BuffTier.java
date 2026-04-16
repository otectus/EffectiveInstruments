package com.crims.effectiveinstruments.aura;

import java.util.Locale;

/**
 * Which activation path an {@link AuraPreset} may be applied from.
 * <ul>
 *   <li>{@link #STATIONARY} — screen-driven instrument play via Genshin Instruments / EMI.
 *   Selected by the player in the aura overlay, gated by the note-activity window.</li>
 *   <li>{@link #MOBILE} — passive tier driven by held Immersive Melodies instruments with
 *   server-side {@code playing=true} NBT state. Resolved via the mobile instrument mapping,
 *   never shown in the selector UI.</li>
 * </ul>
 * Presets may support either tier or both; the JSON field is {@code tiers}.
 */
public enum BuffTier {
    STATIONARY,
    MOBILE;

    /**
     * Case-insensitive parse used by {@link AuraJsonLoader}. Returns {@code null} for
     * unknown strings so the loader can warn and skip without breaking the set.
     */
    public static BuffTier fromJson(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "stationary" -> STATIONARY;
            case "mobile" -> MOBILE;
            default -> null;
        };
    }
}
