package com.leclowndu93150.craftingstationjei.network;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Craftingstationjei.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(C2SScrollPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SScrollPacket::encode)
                .decoder(C2SScrollPacket::new)
                .consumerMainThread(C2SScrollPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CSideSetSideContainerSlot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSideSetSideContainerSlot::encode)
                .decoder(S2CSideSetSideContainerSlot::new)
                .consumerMainThread(S2CSideSetSideContainerSlot::handle)
                .add();
    }
}
