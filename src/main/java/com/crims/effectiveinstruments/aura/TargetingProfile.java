package com.crims.effectiveinstruments.aura;

import java.util.EnumSet;
import java.util.Set;

/**
 * Per-polarity category toggles handed to {@link AuraApplicator#apply}. The
 * MUSICIAN and OWN_PET categories are not included here because their
 * inclusion is hard-wired to polarity (positive always includes them,
 * offensive always excludes them — see
 * {@link AuraApplicator#gatherTargets}). Everything else is config-driven.
 *
 * <p>Construction is typically through the {@link #positive} / {@link #offensive}
 * factories in {@link AuraManager} and {@link com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler},
 * which read the appropriate server-config block.
 */
public record TargetingProfile(
        Set<EntityCategory> allowedCategories,
        int maxTargetsPerTick,
        boolean offensive
) {
    /**
     * Convenience constructor for tests and call sites that build profiles by hand.
     * Defensive-copies the category set so the record's value can't be mutated
     * after construction.
     */
    public TargetingProfile {
        allowedCategories = EnumSet.copyOf(allowedCategories);
    }
}
