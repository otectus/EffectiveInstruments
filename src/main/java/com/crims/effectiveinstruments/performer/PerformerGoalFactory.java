package com.crims.effectiveinstruments.performer;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Per-mod factory that decides whether the adapter contributes a {@link Goal}
 * (goalSelector mods like Recruits, Guard Villagers, Easy NPC) or a
 * {@link BehaviorControl} (Brain-driven mods like Touhou Maid, MCA).
 *
 * <p>Phase 0 ships the interface and reusable bases under {@code performer.ai};
 * Phase 4 wires Brain support; Phases 2-3 implement Goal-based adapters.
 */
public interface PerformerGoalFactory {

    enum Kind { GOAL, BEHAVIOR }

    Kind kind();

    /** GoalSelector mods. */
    default Goal createGoal(Mob host, IAuraPerformer wrapped) {
        throw new UnsupportedOperationException(getClass().getName() + " did not implement createGoal");
    }

    /** Brain mods. */
    default BehaviorControl<LivingEntity> createBehavior(LivingEntity host, IAuraPerformer wrapped) {
        throw new UnsupportedOperationException(getClass().getName() + " did not implement createBehavior");
    }

    int priority();

    EnumSet<Goal.Flag> mutexFlags();
}
