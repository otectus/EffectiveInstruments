package com.crims.effectiveinstruments.durability;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JUnit coverage of:
 * <ul>
 *   <li>{@link InstrumentNamespaces#INSTRUMENT_MOD_IDS} membership — the
 *       1.4.9 fallback (RECS §1.1) only synthesizes a default Entry for items
 *       in this allowlist, so any change here must be deliberate.</li>
 *   <li>The fallback-Entry math used by
 *       {@code InstrumentDurability.synthesizeDefaultEntry}: clamp max to ≥1,
 *       repair-per-unit to ≥1, and {@code defaultMax/5}.</li>
 *   <li>Damage / repair clamping math used by {@code InstrumentDurability}.</li>
 * </ul>
 *
 * <p>Tests that need to call into the real {@code InstrumentDurability}
 * methods would have to spin up a Forge runtime to read EIServerConfig and
 * mock ItemStack/CompoundTag — out of scope for pure JUnit. Mirroring the
 * arithmetic locally matches the existing
 * {@link com.crims.effectiveinstruments.aura.AuraApplicatorBehaviorTest}
 * pattern.
 */
class InstrumentDurabilityNamespaceTest {

    // --- Namespace allowlist contract ----------------------------------------

    @Test
    void allowlistContainsKnownInstrumentMods() {
        Set<String> ids = InstrumentNamespaces.INSTRUMENT_MOD_IDS;
        assertTrue(ids.contains("genshinstrument"));
        assertTrue(ids.contains("evenmoreinstruments"));
        assertTrue(ids.contains("immersive_melodies"));
    }

    @Test
    void allowlistRejectsVanillaAndUnknownNamespaces() {
        Set<String> ids = InstrumentNamespaces.INSTRUMENT_MOD_IDS;
        assertFalse(ids.contains("minecraft"),
                "Vanilla items must not get synthesized durability — bar would render on swords/shields/etc.");
        assertFalse(ids.contains("forge"));
        assertFalse(ids.contains(""));
    }

    @Test
    void allowlistIsImmutable_size() {
        // Smoke alarm: bumping the list size by accident (e.g. test mod id
        // landed in the constant) shows up here. Size matches CLAUDE.md
        // "Quick Reference" claim of three instrument-source mods.
        assertEquals(3, InstrumentNamespaces.INSTRUMENT_MOD_IDS.size());
    }

    // --- Fallback-Entry synthesis math ---------------------------------------

    /**
     * Mirror of {@code InstrumentDurability.synthesizeDefaultEntry}: max is
     * clamped to at least 1, repairPerUnit is max(1, defaultMax/5).
     */
    private static int repairPerUnitForDefaultMax(int defaultMax) {
        return Math.max(1, Math.max(1, defaultMax) / 5);
    }

    @Test
    void defaultMax_normalCase_repairIsOneFifth() {
        assertEquals(240, repairPerUnitForDefaultMax(1200));
        assertEquals(160, repairPerUnitForDefaultMax(800));
    }

    @Test
    void defaultMax_tinyValue_repairFloorsToOne() {
        // Pathological: defaultMax=1 → 1/5=0 → must clamp to 1 so /repair
        // can still make progress (and doesn't divide-by-zero downstream).
        assertEquals(1, repairPerUnitForDefaultMax(1));
        assertEquals(1, repairPerUnitForDefaultMax(4));
    }

    @Test
    void defaultMax_zeroOrNegative_clampsBeforeDivide() {
        // Forge spec.defineInRange enforces min≥1 in practice, but the
        // production code defends with Math.max(1, defaultMax) anyway —
        // mirror that defense here so the contract is captured.
        assertEquals(1, repairPerUnitForDefaultMax(0));
        assertEquals(1, repairPerUnitForDefaultMax(-100));
    }

    // --- Damage / repair clamping math ---------------------------------------

    /** Mirror of {@code InstrumentDurability.damage} clamping at 0. */
    private static int damageResult(int before, int amount) {
        return Math.max(0, before - amount);
    }

    /** Mirror of {@code InstrumentDurability.repair} clamping at max. */
    private static int repairResult(int before, int amount, int max) {
        return Math.min(max, before + amount);
    }

    @Test
    void damageClampsAtZero() {
        assertEquals(0, damageResult(5, 100));
        assertEquals(0, damageResult(0, 1));
        assertEquals(94, damageResult(100, 6));
    }

    @Test
    void repairClampsAtMax() {
        assertEquals(1200, repairResult(1100, 500, 1200));
        assertEquals(1200, repairResult(1200, 1, 1200));
        assertEquals(50, repairResult(0, 50, 1200));
    }

    /** Broken-state gate: production checks {@code current <= 0} after damage. */
    @Test
    void brokenStateTransition_zeroOrLess() {
        assertTrue(damageResult(1, 1) == 0, "damage that brings to 0 should mark broken");
        assertTrue(damageResult(1, 100) == 0, "over-damage should mark broken");
        assertFalse(damageResult(2, 1) == 0, "1 of 2 left is not broken");
    }
}
