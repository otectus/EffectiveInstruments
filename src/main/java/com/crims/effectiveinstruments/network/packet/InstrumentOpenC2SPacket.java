package com.crims.effectiveinstruments.network.packet;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → server: screen-open annotation. Two routing modes, distinguished by
 * the {@code mobileTier} flag:
 * <ul>
 *   <li>Stationary (flag false) — standard flow, routed to
 *       {@link AuraManager#onInstrumentIdReceived}. Auto-selects the default
 *       aura for the instrument.</li>
 *   <li>Mobile (flag true) — IM screen opened/closed. Flips a per-player
 *       "screen open" state on the mobile aura handler so the aura applies
 *       even during free-play (when IM doesn't set {@code playing=true} on
 *       the stack). The {@code close} flag distinguishes open from close.</li>
 * </ul>
 */
public class InstrumentOpenC2SPacket {
    private final ResourceLocation instrumentId;
    private final boolean mobileTier;
    private final boolean close;

    /** Back-compat constructor: stationary-tier, open. */
    public InstrumentOpenC2SPacket(ResourceLocation instrumentId) {
        this(instrumentId, false, false);
    }

    public InstrumentOpenC2SPacket(ResourceLocation instrumentId, boolean mobileTier) {
        this(instrumentId, mobileTier, false);
    }

    public InstrumentOpenC2SPacket(ResourceLocation instrumentId, boolean mobileTier, boolean close) {
        this.instrumentId = instrumentId;
        this.mobileTier = mobileTier;
        this.close = close;
    }

    public static void encode(InstrumentOpenC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.instrumentId);
        buf.writeBoolean(msg.mobileTier);
        buf.writeBoolean(msg.close);
    }

    public static InstrumentOpenC2SPacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        boolean mobile = buf.readableBytes() > 0 && buf.readBoolean();
        boolean closing = buf.readableBytes() > 0 && buf.readBoolean();
        return new InstrumentOpenC2SPacket(id, mobile, closing);
    }

    public static void handle(InstrumentOpenC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            if (msg.mobileTier) {
                handleMobile(sender, msg);
                return;
            }

            // --- Stationary path (pre-1.4.3 behavior) ---

            AuraManager.PlayerAuraState state = AuraManager.getState(sender.getUUID());
            long now = sender.level().getGameTime();

            if (state != null) {
                if (now - state.getLastOpenPacketTick() < 5) return;
                state.markOpenPacketTime(now);
            }

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

    private static void handleMobile(ServerPlayer sender, InstrumentOpenC2SPacket msg) {
        if (msg.close) {
            ImmersiveMelodiesAuraHandler.onScreenClosed(sender, msg.instrumentId);
        } else {
            ImmersiveMelodiesAuraHandler.onScreenOpened(sender, msg.instrumentId);
        }
    }
}
