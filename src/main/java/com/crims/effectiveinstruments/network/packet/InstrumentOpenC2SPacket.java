package com.crims.effectiveinstruments.network.packet;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
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

            // Trust boundary: this packet only *annotates* the current
            // instrument ID and triggers default-aura auto-selection. The
            // authoritative instrument-open flag is set exclusively by the
            // server-side InstrumentOpenStateChangedEvent handler, so a
            // spoofed packet cannot start applying effects — the aura tick
            // requires the authoritative flag before doing anything.
            AuraManager.PlayerAuraState state = AuraManager.getState(sender.getUUID());
            long now = sender.level().getGameTime();

            // Rate limit: 5 tick cooldown (~250ms), tracked independently of
            // aura selection so the two packet types cannot starve each other.
            if (state != null) {
                if (now - state.getLastOpenPacketTick() < 5) return;
                state.markOpenPacketTime(now);
            }

            // Log suspicious instrument IDs from unknown namespaces
            String namespace = msg.instrumentId.getNamespace();
            if (!"genshinstrument".equals(namespace) && !"evenmoreinstruments".equals(namespace)
                    && !"minecraft".equals(namespace)) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Player {} sent instrument open with unusual namespace: {}",
                        sender.getName().getString(), msg.instrumentId);
            }

            AuraManager.onInstrumentIdReceived(sender, msg.instrumentId);
        });
        ctx.setPacketHandled(true);
    }
}
