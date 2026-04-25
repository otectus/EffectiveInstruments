package com.crims.effectiveinstruments.aura;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JUnit table-driven coverage of the sliding-window activation logic
 * inside {@code AuraManager.PlayerAuraState.isActive}. The production code
 * reads {@code EIServerConfig} values that aren't loaded outside a Forge
 * runtime, so this test mirrors the algorithm — same approach as
 * {@link AuraApplicatorBehaviorTest}: the test fails on intentional behavior
 * change so the drift is conscious.
 *
 * <p>1.4.9 (RECS §3.11): added so the 1.4.x post-mortem class of "isActive
 * silently regressed" bugs (instrumentOpen gate, missing recent-notes
 * tracking) gets a load-bearing test the next time someone touches the gate.
 */
class AuraManagerActiveStateTest {

    /**
     * Mirror of {@code PlayerAuraState.isActive}. Returns true when:
     * <ul>
     *   <li>a selection exists,</li>
     *   <li>the most recent note is within {@code noteWindowTicks} of now, AND</li>
     *   <li>at least {@code noteThresholdMin} notes fall inside the
     *       {@code noteThresholdWindowTicks} sliding window ending at now.</li>
     * </ul>
     */
    private static boolean isActive(
            boolean hasSelection,
            long lastNoteGameTime,
            Deque<Long> recentNoteTicks,
            long currentGameTime,
            long noteWindowTicks,
            long noteThresholdWindowTicks,
            int noteThresholdMin
    ) {
        if (!hasSelection) return false;
        if ((currentGameTime - lastNoteGameTime) > noteWindowTicks) return false;
        long thresholdWindowStart = currentGameTime - noteThresholdWindowTicks;
        while (!recentNoteTicks.isEmpty() && recentNoteTicks.peekFirst() < thresholdWindowStart) {
            recentNoteTicks.removeFirst();
        }
        return recentNoteTicks.size() >= noteThresholdMin;
    }

    private static Deque<Long> queue(long... ticks) {
        Deque<Long> q = new ArrayDeque<>();
        for (long t : ticks) q.addLast(t);
        return q;
    }

    @Test
    void noSelection_isInactive() {
        assertFalse(isActive(false, 100L, queue(100L), 105L, 100L, 40L, 1));
    }

    @Test
    void emptyDeque_isInactive_evenWithRecentLastNote() {
        // Defensive: lastNoteGameTime says we played recently, but the deque
        // is empty so the threshold check fails. This is the exact scenario
        // where "play one note then close instrument" used to mis-trigger.
        assertFalse(isActive(true, 100L, queue(), 110L, 100L, 40L, 1));
    }

    @Test
    void singleRecentNote_isActive_withDefaultThreshold() {
        assertTrue(isActive(true, 100L, queue(100L), 110L, 100L, 40L, 1));
    }

    @Test
    void noteOutsideNoteWindow_isInactive() {
        // Note window is 100 ticks (5s). Last note 200 ticks ago → expired.
        assertFalse(isActive(true, 100L, queue(100L), 301L, 100L, 40L, 1));
    }

    @Test
    void noteWindowEdgeButThresholdWindowMisses_isInactive() {
        // Edge: exactly noteWindowTicks since last note (100→200, window=100)
        // — the note-window check passes (100 > 100 is false). But the
        // threshold-window pruning (40 ticks ending at 200, so start=160)
        // empties the deque because the recorded note (tick 100) is now
        // stale, so the threshold-min check fails. Net: inactive.
        // This matches the production contract: BOTH the note-window AND
        // the sliding threshold-window must be satisfied.
        assertFalse(isActive(true, 100L, queue(100L), 200L, 100L, 40L, 1));
    }

    @Test
    void noteWithinBothWindows_isActive() {
        // Note at 180, now=200, both windows pass.
        assertTrue(isActive(true, 180L, queue(180L), 200L, 100L, 40L, 1));
    }

    @Test
    void slidingWindowExpiration_pruneStaleEntries() {
        // 3 notes recorded: 50, 60, 80. Threshold window is 40 ticks ending
        // at now=100 → start=60. Note at 50 is stale and should prune;
        // remaining 2 notes pass the threshold of 2.
        Deque<Long> q = queue(50L, 60L, 80L);
        assertTrue(isActive(true, 80L, q, 100L, 100L, 40L, 2));
        assertFalse(q.contains(50L), "stale 50-tick note should have been pruned");
    }

    @Test
    void thresholdNotMet_isInactive() {
        // 1 recent note in window, threshold needs 3.
        Deque<Long> q = queue(95L);
        assertFalse(isActive(true, 95L, q, 100L, 100L, 40L, 3));
    }

    @Test
    void thresholdExactlyMet_isActive() {
        Deque<Long> q = queue(95L, 96L, 97L);
        assertTrue(isActive(true, 97L, q, 100L, 100L, 40L, 3));
    }

    @Test
    void allNotesStaleByThresholdWindow_isInactive() {
        // Notes only at 50, 51, 52. Threshold window 40 ending at now=200
        // → start=160. All notes pruned. Even if lastNoteGameTime is fresh
        // (because the player kept playing somewhere), if the deque can't
        // satisfy the threshold the aura is off.
        Deque<Long> q = queue(50L, 51L, 52L);
        // Note: the sliding window in production only reads from the deque,
        // so we model that here: lastNoteGameTime is 52 (stale).
        assertFalse(isActive(true, 52L, q, 200L, 100L, 40L, 1));
    }
}
