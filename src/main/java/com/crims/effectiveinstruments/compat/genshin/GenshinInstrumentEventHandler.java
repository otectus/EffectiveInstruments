package com.crims.effectiveinstruments.compat.genshin;

import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.crims.effectiveinstruments.event.StationaryInstrumentNoteService;
import com.cstav.genshinstrument.event.HeldNoteSoundPlayedEvent;
import com.cstav.genshinstrument.event.InstrumentOpenStateChangedEvent;
import com.cstav.genshinstrument.event.InstrumentPlayedEvent;
import com.cstav.genshinstrument.event.NoteSoundPlayedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Quarantined Genshin Instruments event handler. Only loaded when GI is
 * present at runtime — registered manually by
 * {@link GenshinInstrumentsCompat#initCommon()}. <b>Not</b> annotated with
 * {@link net.minecraftforge.fml.common.Mod.EventBusSubscriber}; relying on
 * Forge's class scan would link this class on every install regardless of
 * whether GI is present.
 *
 * <p>All three handlers are thin: they unwrap the GI event into a
 * {@link ServerPlayer} (+ optional instrument id) and call into
 * {@link StationaryInstrumentNoteService} or {@link AuraManager}. Anything
 * shared with the IM tier lives in those services so it remains backend-
 * agnostic.
 */
public final class GenshinInstrumentEventHandler {

    // 1.4.7: GI's InstrumentPlayedEvent is abstract. Forge's EventBus *usually*
    // dispatches concrete subclass posts to abstract-parent listeners, but that
    // behaviour has been reported fragile in corner cases. Subscribe to both
    // concrete subclasses (NoteSoundPlayedEvent, HeldNoteSoundPlayedEvent)
    // explicitly to guarantee delivery. The shared logic lives in processNote.
    @SubscribeEvent
    public static void onNoteSoundPlayed(NoteSoundPlayedEvent event) {
        processNote(event);
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(HeldNoteSoundPlayedEvent event) {
        processNote(event);
    }

    private static void processNote(InstrumentPlayedEvent<?> event) {
        // Only process server-side, player-initiated notes
        if (event.level().isClientSide()) return;
        if (!event.isByPlayer()) return;

        event.entityInfo().ifPresent(entityInfo -> {
            Entity entity = entityInfo.entity;
            if (!(entity instanceof ServerPlayer serverPlayer)) return;

            ResourceLocation instrumentId = null;
            if (event.soundMeta() != null) {
                instrumentId = event.soundMeta().instrumentId();
            }

            boolean cancel = StationaryInstrumentNoteService.processPlayedNote(serverPlayer, instrumentId);
            if (cancel) {
                // InstrumentPlayedEvent is @Cancelable in Genshin Instruments 5.0
                // (verified via bytecode inspection — see CHANGELOG 1.4.0 notes).
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onInstrumentStateChanged(InstrumentOpenStateChangedEvent event) {
        if (event.player.level().isClientSide()) return;

        if (event.isOpen) {
            // Broken-state gate on open: InstrumentOpenStateChangedEvent is not
            // @Cancelable (verified against genshinstrument-5.0), so we can't keep
            // the screen closed — but we can skip registering the player as an
            // active musician and warn them. The note-level gate in
            // StationaryInstrumentNoteService will still block any notes they try to play.
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

    private GenshinInstrumentEventHandler() {}
}
