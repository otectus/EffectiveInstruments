package com.crims.effectiveinstruments.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.cstav.genshinstrument.event.InstrumentPlayedEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = EffectiveInstrumentsMod.MODID)
public class NoteActivityHandler {

    @SubscribeEvent
    public static void onNotePlayed(InstrumentPlayedEvent<?> event) {
        // Only process server-side, player-initiated notes
        if (event.level().isClientSide()) return;
        if (!event.isByPlayer()) return;

        event.entityInfo().ifPresent(entityInfo -> {
            Entity entity = entityInfo.entity;
            if (entity instanceof ServerPlayer serverPlayer) {
                // Fallback: if instrument ID wasn't set by the open packet, capture it from the note
                if (event.soundMeta() != null) {
                    ResourceLocation instrumentId = event.soundMeta().instrumentId();
                    AuraManager.PlayerAuraState state = AuraManager.getState(serverPlayer.getUUID());
                    if (state != null && state.getCurrentInstrumentId() == null && instrumentId != null) {
                        AuraManager.onInstrumentOpenWithId(serverPlayer, instrumentId);
                    }
                }

                AuraManager.onNotePlayed(serverPlayer);
            }
        });
    }
}
