package com.leclowndu93150.craftingstationjei.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PersistantCraftingContainer implements CraftingContainer {

    private final SimpleContainer delegate;
    private final AbstractContainerMenu menu;
    private boolean doNotCallUpdates;

    public PersistantCraftingContainer(AbstractContainerMenu menu, SimpleContainer delegate) {
        this.menu = menu;
        this.delegate = delegate;
    }

    public void setDoNotCallUpdates(boolean flag) {
        this.doNotCallUpdates = flag;
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            items.add(getItem(i));
        }
        return items;
    }

    @Override
    public int getContainerSize() {
        return delegate.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return delegate.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = delegate.removeItem(slot, count);
        if (!stack.isEmpty() && !doNotCallUpdates) {
            menu.slotsChanged(this);
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return delegate.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        delegate.setItem(slot, stack);
        if (!doNotCallUpdates) {
            menu.slotsChanged(this);
        }
    }

    @Override
    public void setChanged() {
        delegate.setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return delegate.stillValid(player);
    }

    @Override
    public void clearContent() {
        delegate.clearContent();
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (int i = 0; i < getContainerSize(); i++) {
            contents.accountSimpleStack(getItem(i));
        }
    }
}
