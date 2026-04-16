package com.crims.effectiveinstruments.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure-function overwrite policy logic used by
 * {@code AuraApplicator.applyEffectSafely}. No Minecraft runtime required.
 */
class OverwritePolicyTest {

    @Test
    void neverOverwriteRefusesEverything() {
        OverwritePolicy p = OverwritePolicy.NEVER_OVERWRITE;
        assertFalse(p.shouldOverwrite(0, 0));
        assertFalse(p.shouldOverwrite(0, 1));
        assertFalse(p.shouldOverwrite(2, 4));
        assertFalse(p.shouldOverwrite(4, 0));
    }

    @Test
    void strongerOnlyRequiresStrictlyGreater() {
        OverwritePolicy p = OverwritePolicy.STRONGER_ONLY;
        assertFalse(p.shouldOverwrite(2, 2), "equal amplifier should NOT overwrite");
        assertFalse(p.shouldOverwrite(2, 1), "weaker amplifier should NOT overwrite");
        assertTrue(p.shouldOverwrite(2, 3),  "stronger amplifier should overwrite");
        assertTrue(p.shouldOverwrite(0, 1),  "stronger amplifier should overwrite");
    }

    @Test
    void refreshTiesOverwritesEqualOrGreater() {
        OverwritePolicy p = OverwritePolicy.REFRESH_TIES;
        assertTrue(p.shouldOverwrite(2, 2),  "equal amplifier should overwrite (refresh duration)");
        assertTrue(p.shouldOverwrite(1, 3),  "stronger amplifier should overwrite");
        assertFalse(p.shouldOverwrite(3, 2), "weaker amplifier should NOT overwrite");
        assertFalse(p.shouldOverwrite(4, 0), "much weaker should NOT overwrite");
    }

    @Test
    void alwaysOverwritesEverything() {
        OverwritePolicy p = OverwritePolicy.ALWAYS;
        assertTrue(p.shouldOverwrite(0, 0));
        assertTrue(p.shouldOverwrite(4, 0), "even much weaker should overwrite");
        assertTrue(p.shouldOverwrite(0, 4));
    }

    @Test
    void refreshTiesIsThePreservedLegacyBehavior() {
        // Before v1.3.0, AuraManager hardcoded: skip if existing.amplifier > new.amplifier.
        // That is REFRESH_TIES. These cases lock in the legacy semantics.
        OverwritePolicy legacy = OverwritePolicy.REFRESH_TIES;
        // existing=2, new=3 → apply (was applied previously)
        assertTrue(legacy.shouldOverwrite(2, 3));
        // existing=2, new=2 → apply (was applied previously — refresh duration)
        assertTrue(legacy.shouldOverwrite(2, 2));
        // existing=3, new=2 → skip (was skipped previously)
        assertFalse(legacy.shouldOverwrite(3, 2));
    }
}
