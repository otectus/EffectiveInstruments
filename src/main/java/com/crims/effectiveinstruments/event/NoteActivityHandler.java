package com.crims.effectiveinstruments.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.cstav.genshinstrument.event.HeldNoteSoundPlayedEvent;
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
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = EffectiveInstrumentsMod.MODID)
public class NoteActivityHandler {

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

            // Fallback: if instrument ID wasn't set by the open packet, capture it from the note
            if (event.soundMeta() != null) {
                ResourceLocation instrumentId = event.soundMeta().instrumentId();
                AuraManager.PlayerAuraState state = AuraManager.getState(serverPlayer.getUUID());
                if (state != null && state.getCurrentInstrumentId() == null && instrumentId != null) {
                    AuraManager.onInstrumentIdReceived(serverPlayer, instrumentId);
                }
            }

            // Broken-state gate: if the held instrument's durability is 0, cancel the
            // note entirely (InstrumentPlayedEvent is @Cancelable in Genshin Instruments
            // 5.0) and nudge the player toward the anvil. No aura, no sound.
            ItemStack instrumentStack = findHeldInstrument(serverPlayer);
            if (instrumentStack != null && InstrumentDurability.isBroken(instrumentStack)) {
                event.setCanceled(true);
                sendBrokenMessageOncePerSecond(serverPlayer);
                // Also clear any stale aura (defensive — damage() does this on break too).
                AuraManager.onInstrumentClose(serverPlayer);
                ImmersiveMelodiesAuraHandler.onExplicitClear(serverPlayer);
                return;
            }

            AuraManager.onNotePlayed(serverPlayer);
            // 1.4.7: apply the aura immediately on each note. The tick-based
            // path (AuraManager.onServerTick) still refreshes effects during
            // held play, but the tick interval (default 10 ticks = 500ms) was
            // too coarse — single-note tests could slip between ticks and feel
            // like the aura wasn't working. Immediate-apply closes that window.
            AuraManager.applyAuraNow(serverPlayer);

            // Durability damage — polarity-aware. Positive aura costs the base amount;
            // negative aura multiplies by the configured offensive multiplier (§5 of the
            // expansion doc). We look up the currently selected aura instead of the
            // event's sound metadata so unselected plays still consume durability.
            if (instrumentStack != null && EIServerConfig.DURABILITY_ENABLED.get()) {
                int cost = resolveCost(serverPlayer);
                if (cost > 0) {
                    int beforeCurrent = InstrumentDurability.getCurrent(instrumentStack);
                    int max = InstrumentDurability.getMax(instrumentStack);
                    boolean brokeNow = InstrumentDurability.damage(instrumentStack, cost, serverPlayer);
                    if (brokeNow) {
                        AuraManager.onInstrumentClose(serverPlayer);
                        ImmersiveMelodiesAuraHandler.onExplicitClear(serverPlayer);
                        sendBrokenMessageOncePerSecond(serverPlayer);
                    } else if (max > 0) {
                        // Crossing-threshold warning at ~10% remaining. Fires once
                        // per second max to avoid flooding chat during a long run.
                        int after = Math.max(0, beforeCurrent - cost);
                        int warnThreshold = Math.max(1, max / 10);
                        if (after <= warnThreshold && beforeCurrent > warnThreshold) {
                            sendLowDurabilityMessageOncePerSecond(serverPlayer, instrumentStack);
                        }
                    }
                }
            }
        });
    }

    /**
     * Best-effort: find a tracked instrument stack on the player. Main hand wins
     * over off hand since Genshin Instruments opens the instrument from main hand
     * by convention.
     */
    @Nullable
    private static ItemStack findHeldInstrument(ServerPlayer player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (InstrumentDurability.isTracked(main)) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (InstrumentDurability.isTracked(off)) return off;
        return null;
    }

    private static int resolveCost(ServerPlayer player) {
        int base = EIServerConfig.DURABILITY_COST_PER_NOTE.get();
        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        if (state == null) return base;
        AuraPreset aura = state.getSelectedAura();
        if (aura != null && aura.isOffensive()) {
            return base * EIServerConfig.OFFENSIVE_DURABILITY_COST_MULT.get();
        }
        return base;
    }

    // Per-player throttle so player A's broken-spam doesn't suppress player B's
    // warning in multiplayer. Keys decay with the player; no explicit cleanup
    // needed beyond logout. Separate maps for broken vs low so the two don't
    // fight each other when durability drops from 10% to 0 in the same second.
    private static final java.util.Map<java.util.UUID, Long> LAST_BROKEN_TICK = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> LAST_LOW_TICK = new java.util.HashMap<>();

    private static void sendBrokenMessageOncePerSecond(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_BROKEN_TICK.get(player.getUUID());
        if (last != null && now - last < 20L) return;
        LAST_BROKEN_TICK.put(player.getUUID(), now);
        player.displayClientMessage(
                Component.translatable("message.effectiveinstruments.durability_broken",
                                player.getMainHandItem().getHoverName())
                        .withStyle(ChatFormatting.RED),
                true // action bar (above hotbar) rather than chat
        );
    }

    private static void sendLowDurabilityMessageOncePerSecond(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        Long last = LAST_LOW_TICK.get(player.getUUID());
        if (last != null && now - last < 20L) return;
        LAST_LOW_TICK.put(player.getUUID(), now);
        player.displayClientMessage(
                Component.translatable("message.effectiveinstruments.durability_low", stack.getHoverName())
                        .withStyle(ChatFormatting.GOLD),
                true
        );
    }

    /** Drop throttle state when a player logs out. Prevents unbounded growth. */
    public static void onPlayerLogout(java.util.UUID playerId) {
        LAST_BROKEN_TICK.remove(playerId);
        LAST_LOW_TICK.remove(playerId);
    }
}
