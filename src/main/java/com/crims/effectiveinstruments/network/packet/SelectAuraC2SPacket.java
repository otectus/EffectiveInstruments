package com.crims.effectiveinstruments.network.packet;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SelectAuraC2SPacket {
    private final String auraId; // empty string = deselect

    public SelectAuraC2SPacket(String auraId) {
        this.auraId = auraId;
    }

    public static void encode(SelectAuraC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.auraId, 256);
    }

    public static SelectAuraC2SPacket decode(FriendlyByteBuf buf) {
        return new SelectAuraC2SPacket(buf.readUtf(256));
    }

    public static void handle(SelectAuraC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            if (msg.auraId.isEmpty()) {
                // Deselect — clear tracked targets, let effects expire naturally
                AuraManager.onAuraSwitch(sender);
                AuraManager.clearAuraSelection(sender);
            } else {
                // Validate the aura ID exists and is enabled
                Optional<AuraPreset> preset = AuraRegistry.getById(msg.auraId);
                if (preset.isEmpty() || !preset.get().enabled()) {
                    EffectiveInstrumentsMod.LOGGER.debug(
                            "Player {} tried to select invalid/disabled aura: {}",
                            sender.getName().getString(), msg.auraId);
                    return;
                }

                // Clear old aura effects before switching
                AuraManager.onAuraSwitch(sender);
                AuraManager.setAuraSelection(sender, preset.get());
            }
        });
        ctx.setPacketHandled(true);
    }
}
