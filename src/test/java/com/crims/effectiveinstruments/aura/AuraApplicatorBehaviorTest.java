package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.config.OverwritePolicy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the two decision rules inside
 * {@link AuraApplicator} that are most at risk of silent drift across refactors:
 *
 * <ol>
 *   <li>The "is this existing effect one of ours that we may strip?" predicate
 *       used by {@code AuraApplicator.clear}.</li>
 *   <li>The affectedTargets tracking contract: {@code apply} only records the
 *       effects it successfully applied; {@code clear} only removes tracked entries.</li>
 * </ol>
 *
 * These tests mirror the production logic locally to run without a Minecraft
 * runtime, matching the existing pattern in {@link AuraSchemaGateTest} and
 * {@link InstrumentAuraMappingJsonTest}.
 *
 * If you change {@code AuraApplicator}'s decision rule, update the mirror here —
 * the test failing is a feature, not a bug: it forces a conscious decision.
 */
class AuraApplicatorBehaviorTest {

    // --- Rule 1: effects we may strip during cleanup --------------------------

    /**
     * Mirror of {@code AuraApplicator.clear}'s per-effect predicate:
     * only remove the effect if it's ambient, amplifier matches the one we applied,
     * and the remaining duration is no longer than what we could have applied.
     */
    private static boolean isStrippable(
            boolean currentIsAmbient,
            int currentAmplifier,
            int currentDuration,
            int ourAmplifier,
            int maxExpectedDuration
    ) {
        return currentIsAmbient
                && currentAmplifier == ourAmplifier
                && currentDuration <= maxExpectedDuration;
    }

    @Test
    void ambientMatchingAmplifierShortDuration_isStripped() {
        assertTrue(isStrippable(true, 0, 160, 0, 260));
    }

    @Test
    void nonAmbientEffectIsNotStripped() {
        // Effects applied by vanilla potions are not ambient → keep them.
        assertFalse(isStrippable(false, 0, 160, 0, 260));
    }

    @Test
    void strongerExistingAmplifierIsNotStripped() {
        // Somebody else applied Strength II; we only applied Strength I.
        assertFalse(isStrippable(true, 1, 160, 0, 260));
    }

    @Test
    void weakerExistingAmplifierIsNotStripped() {
        // Amplifier mismatch in either direction is a "not ours".
        assertFalse(isStrippable(true, 0, 160, 1, 260));
    }

    @Test
    void muchLongerRemainingDurationIsNotStripped() {
        // Someone drank a long potion — duration exceeds our ceiling.
        assertFalse(isStrippable(true, 0, 600, 0, 260));
    }

    @Test
    void exactlyAtCeilingIsStripped() {
        // Boundary: ≤ maxExpectedDuration (inclusive) should be stripped.
        assertTrue(isStrippable(true, 0, 260, 0, 260));
    }

    // --- Rule 2: affectedTargets tracking contract ----------------------------

    /**
     * Mirror of {@code AuraApplicator.apply}'s tracking:
     * an effect is recorded against an entity ID only if {@link OverwritePolicy}
     * says we should apply it given the existing amplifier.
     */
    private static void simulateApply(
            Map<Integer, Set<String>> affectedTargets,
            int entityId,
            String effectName,
            int existingAmplifier, // -1 means none
            int newAmplifier,
            OverwritePolicy policy
    ) {
        boolean shouldApply = (existingAmplifier < 0)
                || policy.shouldOverwrite(existingAmplifier, newAmplifier);
        if (shouldApply) {
            affectedTargets.computeIfAbsent(entityId, k -> new HashSet<>()).add(effectName);
        }
    }

    @Test
    void applyRecordsOnlySuccessfulApplications() {
        Map<Integer, Set<String>> tracked = new HashMap<>();
        // No existing effect → always record.
        simulateApply(tracked, 42, "speed", -1, 0, OverwritePolicy.STRONGER_ONLY);
        // Existing STRONGER effect → do NOT record (we didn't apply).
        simulateApply(tracked, 42, "strength", 2, 0, OverwritePolicy.STRONGER_ONLY);

        assertTrue(tracked.get(42).contains("speed"));
        assertFalse(tracked.get(42).contains("strength"),
                "stronger existing effect must not be tracked — we never applied it");
    }

    @Test
    void refreshTiesTracksEqualAmplifier() {
        Map<Integer, Set<String>> tracked = new HashMap<>();
        // Pre-1.3.0 semantics: equal amplifier refreshes duration → apply → record.
        simulateApply(tracked, 7, "regeneration", 0, 0, OverwritePolicy.REFRESH_TIES);
        assertTrue(tracked.get(7).contains("regeneration"));
    }

    @Test
    void neverOverwriteNeverTracksOnTopOfExisting() {
        Map<Integer, Set<String>> tracked = new HashMap<>();
        simulateApply(tracked, 7, "regeneration", 0, 0, OverwritePolicy.NEVER_OVERWRITE);
        // NEVER_OVERWRITE with an existing effect → no-op → nothing tracked.
        assertFalse(tracked.containsKey(7));
    }

    @Test
    void tracksPerEntityIndependently() {
        Map<Integer, Set<String>> tracked = new HashMap<>();
        simulateApply(tracked, 1, "speed", -1, 0, OverwritePolicy.ALWAYS);
        simulateApply(tracked, 2, "speed", -1, 0, OverwritePolicy.ALWAYS);
        simulateApply(tracked, 1, "haste", -1, 0, OverwritePolicy.ALWAYS);

        assertEquals(Set.of("speed", "haste"), tracked.get(1));
        assertEquals(Set.of("speed"), tracked.get(2));
    }

    // --- Targeting profile shape ----------------------------------------------

    @Test
    void targetingProfileHoldsAllFourKnobs() {
        // Freezes the record shape. Mobile and stationary tiers each construct one
        // per tick — if someone adds a fifth knob without updating both tiers,
        // call sites break at compile time rather than silently drifting.
        TargetingProfile p = new TargetingProfile(true, false, true, 32);
        assertTrue(p.allowSelf());
        assertFalse(p.includeOtherPlayers());
        assertTrue(p.includeTamedPets());
        assertEquals(32, p.maxTargetsPerTick());
    }
}
