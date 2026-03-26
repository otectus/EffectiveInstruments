package com.crims.effectiveinstruments.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.cstav.genshinstrument.event.InstrumentOpenStateChangedEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = EffectiveInstrumentsMod.MODID)
public class InstrumentStateHandler {

    @SubscribeEvent
    public static void onInstrumentStateChanged(InstrumentOpenStateChangedEvent event) {
        if (event.player.level().isClientSide()) return;

        if (event.isOpen) {
            AuraManager.onInstrumentOpen(event.player);
        } else {
            AuraManager.onInstrumentClose(event.player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != LogicalSide.SERVER) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        AuraManager.onServerTick(serverLevel);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AuraManager.onAuraSwitch(sp);
        }
        AuraManager.onPlayerLogout(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Reset instrument-open state on dimension change since the screen closes
        AuraManager.onInstrumentClose(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            AuraManager.onInstrumentClose(player);
        }
    }
}
