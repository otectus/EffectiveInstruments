package com.crims.effectiveinstruments.network;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.network.packet.InstrumentOpenC2SPacket;
import com.crims.effectiveinstruments.network.packet.SelectAuraC2SPacket;
import com.crims.effectiveinstruments.network.packet.SyncAuraSelectionS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class EIPacketHandler {
    private static final String PROTOCOL_VERSION = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EffectiveInstrumentsMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.messageBuilder(SelectAuraC2SPacket.class, id++)
                .encoder(SelectAuraC2SPacket::encode)
                .decoder(SelectAuraC2SPacket::decode)
                .consumerMainThread(SelectAuraC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(InstrumentOpenC2SPacket.class, id++)
                .encoder(InstrumentOpenC2SPacket::encode)
                .decoder(InstrumentOpenC2SPacket::decode)
                .consumerMainThread(InstrumentOpenC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncAuraSelectionS2CPacket.class, id++)
                .encoder(SyncAuraSelectionS2CPacket::encode)
                .decoder(SyncAuraSelectionS2CPacket::decode)
                .consumerMainThread(SyncAuraSelectionS2CPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
