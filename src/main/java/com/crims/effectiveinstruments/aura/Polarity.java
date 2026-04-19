package com.crims.effectiveinstruments.aura;

import java.util.Locale;

/**
 * Whether an {@link AuraPreset} grants a benefit to allies or inflicts a debuff
 * on foes. Polarity decides which {@link TargetingProfile} the caller hands to
 * {@link AuraApplicator#apply} (positive → self+allies, negative → everyone
 * else in range), and drives a red tint on the selector button client-side.
 *
 * <p>Added in 1.4.0. Presets without a {@code "polarity"} field default to
 * {@link #POSITIVE} — the pre-1.4.0 behavior.
 */
public enum Polarity {
    POSITIVE,
    NEGATIVE;

    /**
     * Case-insensitive parse used by {@link AuraJsonLoader}. Returns {@code null}
     * for unknown strings so the loader can warn and fall back to {@link #POSITIVE}.
     */
    public static Polarity fromJson(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "positive", "support", "buff" -> POSITIVE;
            case "negative", "offensive", "debuff" -> NEGATIVE;
            default -> null;
        };
    }
}
