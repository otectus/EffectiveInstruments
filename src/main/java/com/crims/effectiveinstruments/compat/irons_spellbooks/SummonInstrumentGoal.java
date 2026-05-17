package com.crims.effectiveinstruments.compat.irons_spellbooks;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Iron's Spells summon goal. Priority 8 — summons are inherently aggressive
 * and Iron's combat goals own priorities 1-7. Performing only when no
 * target is set is critical because the summon's combat AI uses spell
 * targeting which would mid-cast through our note swing.
 */
public final class SummonInstrumentGoal extends PlayInstrumentGoal {

    public SummonInstrumentGoal(Mob summon) {
        super(summon, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (IronsSpellbooksReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
