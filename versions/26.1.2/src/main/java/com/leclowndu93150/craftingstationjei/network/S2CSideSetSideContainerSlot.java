package com.leclowndu93150.craftingstationjei.network;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CSideSetSideContainerSlot(ItemStack stack, Direction direction, int slot) implements CustomPacketPayload {

    public static final Type<S2CSideSetSideContainerSlot> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftingstationjei.MODID, "side_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSideSetSideContainerSlot> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, S2CSideSetSideContainerSlot::stack,
                    ByteBufCodecs.INT.map(Direction::from3DDataValue, Direction::get3DDataValue), S2CSideSetSideContainerSlot::direction,
                    ByteBufCodecs.INT, S2CSideSetSideContainerSlot::slot,
                    S2CSideSetSideContainerSlot::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CSideSetSideContainerSlot packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().containerMenu instanceof CraftingStationMenu menu) {
                menu.handleSideSlotUpdate(packet.stack(), packet.direction(), packet.slot());
            }
        });
    }
}
