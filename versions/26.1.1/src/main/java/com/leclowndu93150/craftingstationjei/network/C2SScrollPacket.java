package com.leclowndu93150.craftingstationjei.network;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SScrollPacket(int firstSlot) implements CustomPacketPayload {

    public static final Type<C2SScrollPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftingstationjei.MODID, "scroll"));

    public static final StreamCodec<ByteBuf, C2SScrollPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, C2SScrollPacket::firstSlot, C2SScrollPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SScrollPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof CraftingStationMenu menu) {
                menu.setFirstSlot(packet.firstSlot());
            }
        });
    }
}
