package com.crims.effectiveinstruments.aura;

/**
 * The targeting knobs a tier contributes to {@link AuraApplicator#apply}. Each tier
 * (stationary / mobile) reads its own config group and constructs one of these per
 * tick, so the applicator stays tier-agnostic.
 */
public record TargetingProfile(
        boolean allowSelf,
        boolean includeOtherPlayers,
        boolean includeTamedPets,
        int maxTargetsPerTick
) {}
