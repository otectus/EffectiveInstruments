package com.crims.effectiveinstruments.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.cstav.genshinstrument.event.InstrumentOpenStateChangedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
            // Broken-state gate on open: InstrumentOpenStateChangedEvent is not
            // @Cancelable (verified against genshinstrument-5.0), so we can't keep
            // the screen closed — but we can skip registering the player as an
            // active musician and warn them. The note-level gate in
            // NoteActivityHandler will still block any notes they try to play.
            if (event.player instanceof ServerPlayer sp && isHoldingBrokenInstrument(sp)) {
                sp.displayClientMessage(
                        Component.literal("This instrument is broken — repair it on an anvil to play again.")
                                .withStyle(ChatFormatting.RED),
                        false // chat (the overlay screen covers the action bar)
                );
                return;
            }
            AuraManager.onInstrumentOpen(event.player);
        } else {
            AuraManager.onInstrumentClose(event.player);
        }
    }

    private static boolean isHoldingBrokenInstrument(ServerPlayer player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (InstrumentDurability.isBroken(main)) return true;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        return InstrumentDurability.isBroken(off);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != LogicalSide.SERVER) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        AuraManager.onServerTick(serverLevel);
        // Mobile tier runs after the stationary tick so its suppression check reads fresh state.
        ImmersiveMelodiesAuraHandler.onServerTick(serverLevel);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AuraManager.onAuraSwitch(sp);
        }
        AuraManager.onPlayerLogout(event.getEntity().getUUID());
        ImmersiveMelodiesAuraHandler.onPlayerLogout(event.getEntity().getUUID());
        com.crims.effectiveinstruments.event.NoteActivityHandler.onPlayerLogout(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Reset instrument-open state on dimension change since the screen closes.
        // Mobile tier intentionally does NOT force-clear here — it matches the stationary
        // tier's "effects expire naturally across dimensions" comment in AuraManager.
        AuraManager.onInstrumentClose(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            AuraManager.onInstrumentClose(player);
            if (player instanceof ServerPlayer sp) {
                ImmersiveMelodiesAuraHandler.onExplicitClear(sp);
            }
        }
    }
}
