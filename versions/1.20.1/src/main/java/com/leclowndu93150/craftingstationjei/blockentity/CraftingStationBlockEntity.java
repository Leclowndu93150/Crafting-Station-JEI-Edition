package com.leclowndu93150.craftingstationjei.blockentity;

import com.leclowndu93150.craftingstationjei.init.ModBlockEntityTypes;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class CraftingStationBlockEntity extends BlockEntity implements MenuProvider {

    private final SimpleContainer input = new SimpleContainer(9);
    private Direction currentContainer = null;
    private Component customName = null;

    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CRAFTING_STATION.get(), pos, state);
        this.input.addListener(container -> {
            if (this.level != null && !this.level.isClientSide) {
                setChanged();
                BlockState s = this.level.getBlockState(this.worldPosition);
                this.level.sendBlockUpdated(this.worldPosition, s, s, 3);
            }
        });
    }

    public SimpleContainer getInput() {
        return input;
    }

    public Direction getCurrentContainer() {
        return currentContainer;
    }

    public void setCurrentContainer(Direction direction) {
        this.currentContainer = direction;
        setChanged();
    }

    public void setCustomName(Component name) {
        this.customName = name;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : Component.translatable("block.craftingstationjei.crafting_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStationMenu(containerId, playerInventory, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < 9; i++) {
            if (tag.contains("Slot" + i)) {
                input.setItem(i, net.minecraft.world.item.ItemStack.of(tag.getCompound("Slot" + i)));
            }
        }
        if (tag.contains("CurrentContainer")) {
            currentContainer = Direction.from3DDataValue(tag.getInt("CurrentContainer"));
        }
        if (tag.contains("CustomName")) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < 9; i++) {
            tag.put("Slot" + i, input.getItem(i).save(new CompoundTag()));
        }
        if (currentContainer != null) {
            tag.putInt("CurrentContainer", currentContainer.get3DDataValue());
        }
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
