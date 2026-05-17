package com.crims.effectiveinstruments.compat.guardvillagers;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Guard-specific play goal. Higher mutex priority than Recruits (5 vs 3) so
 * raid alerts / shield-raise / melee goals preempt cleanly per spec §6.2.
 */
public final class GuardInstrumentGoal extends PlayInstrumentGoal {

    public GuardInstrumentGoal(Mob guard) {
        super(guard, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (GuardVillagersReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
