package com.crims.effectiveinstruments.config;

/**
 * How aura effect application interacts with pre-existing effects on targets.
 * Pulled out of {@link EIServerConfig} so tests can reference it without
 * triggering ForgeConfigSpec static initialization.
 *
 * <ul>
 *   <li>{@link #NEVER_OVERWRITE} — only apply when target has no effect of this type.</li>
 *   <li>{@link #STRONGER_ONLY}   — overwrite only if new amplifier is strictly greater.</li>
 *   <li>{@link #REFRESH_TIES}    — overwrite if new amplifier &gt;= existing (default).</li>
 *   <li>{@link #ALWAYS}          — always overwrite regardless of existing amplifier.</li>
 * </ul>
 */
public enum OverwritePolicy {
    NEVER_OVERWRITE,
    STRONGER_ONLY,
    REFRESH_TIES,
    ALWAYS;

    /** Pure function. Returns true if the new amplifier should overwrite the existing one. */
    public boolean shouldOverwrite(int existingAmplifier, int newAmplifier) {
        return switch (this) {
            case NEVER_OVERWRITE -> false;
            case STRONGER_ONLY   -> newAmplifier > existingAmplifier;
            case REFRESH_TIES    -> newAmplifier >= existingAmplifier;
            case ALWAYS          -> true;
        };
    }
}
