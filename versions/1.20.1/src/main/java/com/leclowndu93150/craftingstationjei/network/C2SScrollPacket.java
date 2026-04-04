package com.leclowndu93150.craftingstationjei.network;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SScrollPacket {

    private final int firstSlot;

    public C2SScrollPacket(int firstSlot) {
        this.firstSlot = firstSlot;
    }

    public C2SScrollPacket(FriendlyByteBuf buf) {
        this.firstSlot = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(firstSlot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.containerMenu instanceof CraftingStationMenu menu) {
            menu.setFirstSlot(firstSlot);
        }
    }
}
