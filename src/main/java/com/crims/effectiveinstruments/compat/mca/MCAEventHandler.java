package com.crims.effectiveinstruments.compat.mca;

import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge-bus event handler for MCA villagers. 1.6.0 hotfix #5 introduces this
 * to support Tier-1 (full-performer) promotion: injects the play goal at
 * entity-join time and cleans state on death.
 */
public final class MCAEventHandler {

    private static final int GOAL_PRIORITY = 8;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EIServerConfig.NPCS_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!MCAReflection.isMcaVillager(mob)) return;
        try {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new MCAInstrumentGoal(mob));
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!MCAReflection.isMcaVillager(event.getEntity())) return;
        ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(event.getEntity().getUUID());
    }

    private MCAEventHandler() {}
}
