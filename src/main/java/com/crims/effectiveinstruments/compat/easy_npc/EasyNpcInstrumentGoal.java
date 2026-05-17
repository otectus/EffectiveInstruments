package com.crims.effectiveinstruments.compat.easy_npc;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Easy NPC goal. Priority 4 — Easy NPC's Objective-driven goals can include
 * attack and follow targets; combat goals typically own priority 1-3 in the
 * NPC's profession kit so 4 sits safely below them.
 *
 * <p>Note: Easy NPC profession swaps rebuild the goalSelector via
 * {@code ObjectiveManager}. Spec §6.3 calls for re-injection via a periodic
 * sweep — Phase 3 ships goal injection on join; the periodic sweep can be
 * added if testing surfaces a swap-loses-goal regression.
 */
public final class EasyNpcInstrumentGoal extends PlayInstrumentGoal {

    public EasyNpcInstrumentGoal(Mob npc) {
        super(npc, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (EasyNpcReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
