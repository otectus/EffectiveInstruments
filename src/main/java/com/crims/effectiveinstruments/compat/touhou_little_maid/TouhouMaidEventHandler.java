package com.crims.effectiveinstruments.compat.touhou_little_maid;

import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TouhouMaidEventHandler {

    private static final int GOAL_PRIORITY = 7;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EIServerConfig.NPCS_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!TouhouMaidReflection.isMaid(mob)) return;
        try {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new MaidInstrumentGoal(mob));
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!TouhouMaidReflection.isMaid(event.getEntity())) return;
        ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(event.getEntity().getUUID());
    }

    private TouhouMaidEventHandler() {}
}
