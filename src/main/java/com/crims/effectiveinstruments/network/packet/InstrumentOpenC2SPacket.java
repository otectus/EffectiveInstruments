package com.crims.effectiveinstruments.network.packet;

import com.crims.effectiveinstruments.aura.AuraManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class InstrumentOpenC2SPacket {
    private final ResourceLocation instrumentId;

    public InstrumentOpenC2SPacket(ResourceLocation instrumentId) {
        this.instrumentId = instrumentId;
    }

    public static void encode(InstrumentOpenC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.instrumentId);
    }

    public static InstrumentOpenC2SPacket decode(FriendlyByteBuf buf) {
        return new InstrumentOpenC2SPacket(buf.readResourceLocation());
    }

    public static void handle(InstrumentOpenC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            AuraManager.onInstrumentOpenWithId(sender, msg.instrumentId);
        });
        ctx.setPacketHandled(true);
    }
}
