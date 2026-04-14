package com.leclowndu93150.craftingstationjei.menu;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class SideContainerWrapper {

    @FunctionalInterface
    public interface SetStackHook {
        boolean apply(IItemHandler handler, int slot, ItemStack stack);
    }

    public static SetStackHook EXTRA_SET_STACK = (h, s, st) -> false;

    private final IItemHandler handler;

    public static final SideContainerWrapper EMPTY = new SideContainerWrapper(new IItemHandler() {
        @Override public int getSlots() { return 0; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    });

    public SideContainerWrapper(IItemHandler handler) {
        this.handler = handler;
    }

    public int getSlotCount() {
        return handler.getSlots();
    }

    public ItemStack getStack(int slot) {
        return handler.getStackInSlot(slot);
    }

    public void setStack(int slot, ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
            return;
        }
        EXTRA_SET_STACK.apply(handler, slot, stack);
    }

    public ItemStack removeStack(int slot, int count) {
        return handler.extractItem(slot, count, false);
    }

    public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return handler.insertItem(slot, stack, simulate);
    }

    public int getMaxStackSize(int slot) {
        return handler.getSlotLimit(slot);
    }

    public boolean valid(int slot) {
        return slot >= 0 && slot < getSlotCount();
    }

    public IItemHandler getHandler() {
        return handler;
    }
}
