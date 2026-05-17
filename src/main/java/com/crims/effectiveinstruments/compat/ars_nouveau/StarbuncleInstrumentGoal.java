package com.crims.effectiveinstruments.compat.ars_nouveau;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Starbuncle goal. Priority 4 — Starbuncle's own transport / wander goals
 * own priorities 1-3 so 4 sits safely below them. The starbuncle pauses
 * when it has a target (vanilla Mob.getTarget) or took damage.
 */
public final class StarbuncleInstrumentGoal extends PlayInstrumentGoal {

    public StarbuncleInstrumentGoal(Mob starby) {
        super(starby, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (ArsNouveauReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
