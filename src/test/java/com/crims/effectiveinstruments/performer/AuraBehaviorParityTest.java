package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.aura.EntityCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 0 parity gate. The full "byte-identical aura behavior" test against
 * the 1.5.0 golden lives in GameTest fixtures (spec §14 {@code ei.parity.vanilla_only}),
 * which requires a Minecraft runtime.
 *
 * <p>This file is the lightweight unit gate that runs in plain JUnit and
 * checks structural invariants: that the new performer abstractions did not
 * accidentally renumber or rename the data shapes the player path depends on.
 * Any failure here is a strong signal that the parity test will fail in-game.
 *
 * <p>What is verified:
 * <ul>
 *   <li>{@link EntityCategory} enum order is the 1.5.0 order. Order matters
 *       because {@code AuraApplicator.gatherTargets} emits in declared order.</li>
 *   <li>{@link EntityCategory#MUSICIAN} and {@link EntityCategory#OWN_PET}
 *       are the first two values — the polarity-hard-wired buckets.</li>
 * </ul>
 */
class AuraBehaviorParityTest {

    @Test
    void entityCategoryOrderUnchangedFrom_1_5_0() {
        EntityCategory[] values = EntityCategory.values();
        assertEquals(8, values.length, "v1.6.0 must not add or remove EntityCategory values");
        assertEquals(EntityCategory.MUSICIAN,         values[0]);
        assertEquals(EntityCategory.OWN_PET,          values[1]);
        assertEquals(EntityCategory.OTHER_PLAYER,     values[2]);
        assertEquals(EntityCategory.OTHER_PLAYER_PET, values[3]);
        assertEquals(EntityCategory.VILLAGER,         values[4]);
        assertEquals(EntityCategory.IRON_GOLEM,       values[5]);
        assertEquals(EntityCategory.PASSIVE_MOB,      values[6]);
        assertEquals(EntityCategory.HOSTILE_MOB,      values[7]);
    }

    @Test
    void targetClassifierExists() {
        // Smoke test: the new TargetClassifier class is present and has the
        // expected static classify(Entity, IAuraPerformer, Set) signature
        // (verified by AuraApplicator referencing it). If this test fails the
        // class was renamed or moved.
        assertNotNull(TargetClassifier.class);
    }

    @Test
    void playerPerformerExists() {
        // The wrapping anchor — every internal caller routes through this.
        assertNotNull(PlayerPerformer.class);
    }

    @Test
    void iAuraPerformerExists() {
        assertNotNull(IAuraPerformer.class);
    }
}
