package com.crims.effectiveinstruments.compat.doggytalents;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Doggy Talents goal. Priority 6 — below DTN's combat goals (1-5) so attack
 * targets preempt cleanly. {@code canUse} requires the dog be sitting OR in
 * docile mode per spec §6.5 acceptance "sitting dog plays".
 */
public final class DogInstrumentGoal extends PlayInstrumentGoal {

    public DogInstrumentGoal(Mob dog) {
        super(dog, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = Math.max(EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get(), 100);
    }

    @Override
    public boolean canUse() {
        if (DoggyTalentsReflection.isInCombatState(host)) return false;
        if (!DoggyTalentsReflection.isDocileOrSitting(host)) return false;
        return super.canUse();
    }
}
