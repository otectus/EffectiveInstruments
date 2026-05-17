package com.crims.effectiveinstruments.compat.doggytalents;

import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DoggyTalentsEventHandler {

    private static final int GOAL_PRIORITY = 6;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EIServerConfig.NPCS_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!DoggyTalentsReflection.isDog(mob)) return;
        try {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new DogInstrumentGoal(mob));
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!DoggyTalentsReflection.isDog(event.getEntity())) return;
        ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(event.getEntity().getUUID());
    }

    private DoggyTalentsEventHandler() {}
}
