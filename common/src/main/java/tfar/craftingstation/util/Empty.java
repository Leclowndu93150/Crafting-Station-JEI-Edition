package tfar.craftingstation.util;

import net.minecraft.world.item.ItemStack;

public class Empty implements SideContainerWrapper {

    public static final Empty EMPTY = new Empty();

    @Override
    public int $getSlotCount() {
        return 0;
    }

    @Override
    public ItemStack $getStack(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void $setStack(int slot, ItemStack stack) {

    }

    @Override
    public ItemStack $removeStack(int slot, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public int $getMaxStackSize(int slot) {
        return 0;
    }

    @Override
    public ItemStack $insert(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }
}
