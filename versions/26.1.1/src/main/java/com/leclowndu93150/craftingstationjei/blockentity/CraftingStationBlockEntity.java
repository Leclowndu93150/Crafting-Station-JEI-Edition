package com.leclowndu93150.craftingstationjei.blockentity;

import com.leclowndu93150.craftingstationjei.init.ModBlockEntityTypes;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public class CraftingStationBlockEntity extends BlockEntity implements MenuProvider {

    private final SimpleContainer input = new SimpleContainer(9);
    private Direction currentContainer = null;
    private Component customName = null;

    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CRAFTING_STATION.get(), pos, state);
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
        return customName != null ? customName : Component.translatable("block.craftingstation.crafting_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStationMenu(containerId, playerInventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 9; i++) {
            this.input.setItem(i, input.read("Slot" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        currentContainer = input.getInt("CurrentContainer")
                .map(Direction::from3DDataValue)
                .orElse(null);
        customName = BlockEntity.parseCustomNameSafe(input, "CustomName");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                output.store("Slot" + i, ItemStack.CODEC, stack);
            }
        }
        if (currentContainer != null) {
            output.putInt("CurrentContainer", currentContainer.get3DDataValue());
        }
        if (customName != null) {
            output.store("CustomName", ComponentSerialization.CODEC, customName);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
