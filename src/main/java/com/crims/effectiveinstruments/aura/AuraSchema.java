package com.crims.effectiveinstruments.aura;

/**
 * Central schema version constants for the mod's JSON data files.
 * Bump {@link #CURRENT_VERSION} whenever a backwards-incompatible field is
 * added or semantics change; the loaders will then surface a migration
 * boundary instead of silently interpreting old data under new rules.
 */
public final class AuraSchema {
    /** Current schema version for aura preset JSON and instrument mapping JSON. */
    public static final int CURRENT_VERSION = 1;

    /** JSON field key used in both aura presets and the instrument mapping. */
    public static final String FIELD = "schemaVersion";

    private AuraSchema() {}
}
