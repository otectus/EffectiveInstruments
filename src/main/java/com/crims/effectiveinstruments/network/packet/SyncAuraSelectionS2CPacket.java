package com.crims.effectiveinstruments.network.packet;

import com.crims.effectiveinstruments.client.event.AuraOverlayInjector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncAuraSelectionS2CPacket {
    private final String auraId;

    public SyncAuraSelectionS2CPacket(String auraId) {
        this.auraId = auraId;
    }

    public static void encode(SyncAuraSelectionS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.auraId, 256);
    }

    public static SyncAuraSelectionS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncAuraSelectionS2CPacket(buf.readUtf(256));
    }

    public static void handle(SyncAuraSelectionS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    AuraOverlayInjector.onServerSyncAura(msg.auraId));
        });
        ctx.setPacketHandled(true);
    }
}
