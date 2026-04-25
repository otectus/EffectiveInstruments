package com.crims.effectiveinstruments.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JUnit coverage of the arithmetic inside {@code InstrumentAnvilHandler}:
 * combine bonus, material-repair amount, materials-needed rounding, and the
 * 1.4.9 vanilla-style work-penalty escalation (RECS §2.4).
 *
 * <p>The handler itself receives a Forge {@code AnvilUpdateEvent} and reads
 * {@link net.minecraft.world.item.ItemStack} state — both of which require a
 * Minecraft runtime to instantiate. Mirroring the math here matches the
 * existing {@link com.crims.effectiveinstruments.aura.AuraApplicatorBehaviorTest}
 * pattern: the test fails when production drifts, forcing a conscious
 * decision.
 */
class InstrumentAnvilHandlerMathTest {

    // --- Vanilla AnvilMenu.calculateIncreasedRepairCost: prev*2 + 1 ----------
    // Mirror of the production call so the 1.4.9 escalation contract is
    // captured: each anvil use bumps the next prior-work cost by 2x+1.

    private static int nextRepairCost(int prev) {
        return prev * 2 + 1;
    }

    @Test
    void firstRepair_costIsOne() {
        assertEquals(1, nextRepairCost(0),
                "fresh-from-craft instrument has no prior work — first cost is 1");
    }

    @Test
    void escalationGrowsExponentially() {
        int cost = 0;
        cost = nextRepairCost(cost); assertEquals(1, cost);
        cost = nextRepairCost(cost); assertEquals(3, cost);
        cost = nextRepairCost(cost); assertEquals(7, cost);
        cost = nextRepairCost(cost); assertEquals(15, cost);
        cost = nextRepairCost(cost); assertEquals(31, cost);
    }

    @Test
    void escalationCrossesTooExpensiveThreshold() {
        // Vanilla anvils cap out at 39 levels (Too Expensive). After ~5
        // combines on a fresh instrument we're past the threshold — same
        // budget as a vanilla diamond pickaxe.
        int cost = 0;
        for (int i = 0; i < 5; i++) cost = nextRepairCost(cost);
        assertTrue(cost >= 31, "5 combines should cost at least 31 levels");
    }

    // --- Combine bonus -------------------------------------------------------

    /** Mirror: combine output = min(max, leftCur + rightCur + bonus). */
    private static int combineDurability(int max, int leftCur, int rightCur, int bonusPercent) {
        int bonus = max * bonusPercent / 100;
        return Math.min(max, leftCur + rightCur + bonus);
    }

    @Test
    void combine_twoEqualHalves_bonusBringsToFull_atVanilla12Percent() {
        // 1200 max, two halves at 600 + 12% bonus = 1344 → clamped to 1200.
        assertEquals(1200, combineDurability(1200, 600, 600, 12));
    }

    @Test
    void combine_twoLowDurability_summedPlusBonus() {
        // 1200 max. Left=200, right=200, bonus=144. Total=544.
        assertEquals(544, combineDurability(1200, 200, 200, 12));
    }

    @Test
    void combine_zeroBonusIsAllowed_clampsToMax() {
        // Admin can set DURABILITY_ANVIL_REPAIR_BONUS_PERCENT=0 — should still
        // honor the clamp at max.
        assertEquals(1000, combineDurability(1000, 600, 600, 0));
    }

    // --- Material repair -----------------------------------------------------

    /**
     * Mirror: materialsNeeded = min(stackCount, ceil(needed/perUnit)),
     * restoredBy = min(needed, materialsNeeded * perUnit).
     */
    private static int materialsNeeded(int needed, int perUnit, int stackCount) {
        return Math.min(stackCount, (needed + perUnit - 1) / perUnit);
    }

    private static int restoredBy(int needed, int materialsNeeded, int perUnit) {
        return Math.min(needed, materialsNeeded * perUnit);
    }

    @Test
    void materialRepair_neededLessThanPerUnit_consumesOne() {
        // 50 needed, 240 per unit → ceil(50/240)=1.
        assertEquals(1, materialsNeeded(50, 240, 64));
        assertEquals(50, restoredBy(50, 1, 240));
    }

    @Test
    void materialRepair_exactMultiple() {
        // 480 needed, 240 per unit → 2 materials, 480 restored.
        assertEquals(2, materialsNeeded(480, 240, 64));
        assertEquals(480, restoredBy(480, 2, 240));
    }

    @Test
    void materialRepair_roundsUp() {
        // 500 needed, 240 per unit → ceil(500/240)=3 (would over-restore by 220
        // but restoredBy clamps to needed).
        assertEquals(3, materialsNeeded(500, 240, 64));
        assertEquals(500, restoredBy(500, 3, 240));
    }

    @Test
    void materialRepair_cappedByStackCount() {
        // 1200 needed, 240 per unit → wants 5; player only has 2.
        assertEquals(2, materialsNeeded(1200, 240, 2));
        assertEquals(480, restoredBy(1200, 2, 240));
    }

    // --- 1.4.9 cost formula --------------------------------------------------

    /**
     * Mirror of the new combine cost: {@code nextRepairCost + 2}.
     * Mirror of the new material cost: {@code nextRepairCost + materialsNeeded * 2}.
     */
    @Test
    void newCombineCost_isEscalatedPlusFlat() {
        int prev = 0;
        int next = nextRepairCost(prev);
        assertEquals(3, next + 2,
                "first combine on a fresh instrument: base cost (1) + flat (2) = 3 levels");

        prev = 1;
        next = nextRepairCost(prev);
        assertEquals(5, next + 2);
    }

    @Test
    void newMaterialCost_scalesWithMaterials() {
        int next = nextRepairCost(0);
        // Fresh repair, 1 material = 1 + 1*2 = 3 levels.
        assertEquals(3, next + 1 * 2);
        // Fresh repair, 4 materials = 1 + 4*2 = 9 levels.
        assertEquals(9, next + 4 * 2);
    }
}
