package com.crims.effectiveinstruments.aura;

/**
 * Central schema version constants for the mod's JSON data files.
 * Bump {@link #CURRENT_VERSION} whenever a backwards-incompatible field is
 * added or semantics change; the loaders will then surface a migration
 * boundary instead of silently interpreting old data under new rules.
 */
public final class AuraSchema {
    /**
     * Current schema version for aura preset JSON and instrument mapping JSON.
     * <ul>
     *   <li>v1 (1.0.0 – 1.3.x) — positive-only presets, string-form mobile mapping.</li>
     *   <li>v2 (1.4.0+) — adds optional {@code "polarity"} field to presets and
     *       accepts object-form entries in {@code mobile_instrument_auras.json}
     *       (for the offensive-aura allow-list). Both additions are backwards-
     *       compatible: v1 files load unchanged.</li>
     * </ul>
     */
    public static final int CURRENT_VERSION = 2;

    /** JSON field key used in both aura presets and the instrument mapping. */
    public static final String FIELD = "schemaVersion";

    private AuraSchema() {}
}
