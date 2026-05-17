package com.crims.effectiveinstruments.compat.irons_spellbooks;

import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class IronsSpellbooksEventHandler {

    private static final int GOAL_PRIORITY = 8;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EIServerConfig.NPCS_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!IronsSpellbooksReflection.isMagicSummon(mob)) return;
        try {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new SummonInstrumentGoal(mob));
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!IronsSpellbooksReflection.isMagicSummon(event.getEntity())) return;
        ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(event.getEntity().getUUID());
    }

    private IronsSpellbooksEventHandler() {}
}
