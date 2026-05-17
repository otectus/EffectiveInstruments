package com.crims.effectiveinstruments.compat.touhou_little_maid;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Touhou Maid play goal. Priority 7 — sits below the maid's combat /
 * task-driven goals. Brain TASK system ticks in parallel and our
 * {@link TouhouMaidReflection#isInCombatState} reads it.
 */
public final class MaidInstrumentGoal extends PlayInstrumentGoal {

    public MaidInstrumentGoal(Mob maid) {
        super(maid, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (TouhouMaidReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
