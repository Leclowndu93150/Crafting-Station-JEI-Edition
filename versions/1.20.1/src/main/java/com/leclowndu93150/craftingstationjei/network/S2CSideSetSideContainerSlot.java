package com.leclowndu93150.craftingstationjei.network;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSideSetSideContainerSlot {

    private final ItemStack stack;
    private final Direction direction;
    private final int slot;

    public S2CSideSetSideContainerSlot(ItemStack stack, Direction direction, int slot) {
        this.stack = stack;
        this.direction = direction;
        this.slot = slot;
    }

    public S2CSideSetSideContainerSlot(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count <= 0) {
            this.stack = ItemStack.EMPTY;
        } else {
            Item item = buf.readById(BuiltInRegistries.ITEM);
            CompoundTag tag = buf.readNbt();
            ItemStack s = new ItemStack(item, count);
            s.setTag(tag);
            this.stack = s;
        }
        this.direction = Direction.from3DDataValue(buf.readVarInt());
        this.slot = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        if (stack.isEmpty()) {
            buf.writeVarInt(0);
        } else {
            buf.writeVarInt(stack.getCount());
            buf.writeId(BuiltInRegistries.ITEM, stack.getItem());
            buf.writeNbt(stack.getTag());
        }
        buf.writeVarInt(direction.get3DDataValue());
        buf.writeVarInt(slot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof CraftingStationMenu menu) {
            menu.handleSideSlotUpdate(stack, direction, slot);
        }
    }
}
