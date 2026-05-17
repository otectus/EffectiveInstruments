package com.crims.effectiveinstruments.compat.guardvillagers;

import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge bus listeners for Guards. Priority 5 keeps us strictly below
 * RaiseShieldGoal/GuardMeleeGoal so combat preempts.
 */
public final class GuardVillagersEventHandler {

    private static final int GOAL_PRIORITY = 5;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EIServerConfig.NPCS_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!GuardVillagersReflection.isGuard(mob)) return;
        try {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new GuardInstrumentGoal(mob));
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!GuardVillagersReflection.isGuard(event.getEntity())) return;
        ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(event.getEntity().getUUID());
    }

    private GuardVillagersEventHandler() {}
}
