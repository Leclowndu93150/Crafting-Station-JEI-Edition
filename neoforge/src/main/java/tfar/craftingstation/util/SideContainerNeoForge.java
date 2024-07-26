package tfar.craftingstation.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class SideContainerNeoForge implements IItemHandlerModifiable,SideContainerWrapper {

    private final IItemHandlerModifiable handler;

    public SideContainerNeoForge(IItemHandlerModifiable handler) {
        this.handler = handler;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if ($valid(slot)) {
            handler.setStackInSlot(slot, stack);
        }
    }


    @Override
    public int getSlots() {
        return handler.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (!$valid(slot)) return ItemStack.EMPTY;
        return handler.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return handler.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!$valid(slot)) return ItemStack.EMPTY;
        return handler.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return handler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return handler.isItemValid(slot,stack);
    }

    @Override
    public int $getSlotCount() {
        return getSlots();
    }

    @Override
    public ItemStack $getStack(int slot) {
        return getStackInSlot(slot);
    }

    @Override
    public void $setStack(int slot, ItemStack stack) {
        setStackInSlot(slot, stack);
    }

    @Override
    public ItemStack $removeStack(int slot, int count) {
        return extractItem(slot,count,false);
    }

    @Override
    public int $getMaxStackSize(int slot) {
        return getSlotLimit(slot);
    }

    @Override
    public ItemStack $insert(int slot, ItemStack stack, boolean simulate) {
        return insertItem(slot, stack, simulate);
    }
}
