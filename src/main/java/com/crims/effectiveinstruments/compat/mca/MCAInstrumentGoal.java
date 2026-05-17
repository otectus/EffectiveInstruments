package com.crims.effectiveinstruments.compat.mca;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.ai.PlayInstrumentGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * MCA villager play goal. Priority 8 — sits well below MCA's combat /
 * profession-driven goals (typically priorities 1-4). The villager's Brain
 * ticks profession behaviors in parallel via {@code customServerAiStep};
 * this goal-based path handles instrument-play independently, falling back
 * to {@link com.crims.effectiveinstruments.event.StationaryInstrumentNoteService}
 * for note processing (or the IM mobile pulser when the instrument is
 * mobile-mapped, via {@link PlayInstrumentGoal}'s auto-registration).
 *
 * <p>1.6.0 hotfix #5 Tier-1 promotion of MCA.
 */
public final class MCAInstrumentGoal extends PlayInstrumentGoal {

    public MCAInstrumentGoal(Mob villager) {
        super(villager, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldownTicks = EIServerConfig.NPCS_PERFORMER_COOLDOWN_TICKS.get();
    }

    @Override
    public boolean canUse() {
        if (MCAReflection.isInCombatState(host)) return false;
        return super.canUse();
    }
}
