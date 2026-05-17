package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Recruits-specific {@link PlayInstrumentGoal} subclass. Adds the Recruits
 * gates spelled out in spec §6.1:
 * <ul>
 *   <li>Target null (already in {@link PlayInstrumentGoal#canUse()}).</li>
 *   <li>{@link RecruitsReflection#isInCombatState(net.minecraft.world.entity.LivingEntity)
 *       isInCombatState} returns false — covers raid alert, fleeing, taking
 *       damage.</li>
 *   <li>Owner-online gate honored via {@link RecruitPerformer#canPerformNow}.</li>
 * </ul>
 *
 * <p>Mutex flags = {@link Goal.Flag#MOVE}, {@link Goal.Flag#LOOK} — combat
 * goals naturally preempt because they own the same mutex set.
 *
 * <p>Cooldown ticks default to the global {@code npcs.performerCooldownTicks}
 * value (60 by default).
 */
public final class RecruitInstrumentGoal extends PlayInstrumentGoal {

    public RecruitInstrumentGoal(Mob recruit) {
        super(recruit, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (RecruitsReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
