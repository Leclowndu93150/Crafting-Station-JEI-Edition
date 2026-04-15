package com.leclowndu93150.craftingstationjei.compat;

import com.buuz135.functionalstorage.inventory.BigInventoryHandler;
import com.buuz135.functionalstorage.inventory.ControllerInventoryHandler;
import com.leclowndu93150.craftingstationjei.menu.SideContainerWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class FunctionalStorageCompat {

    public static void init() {
        SideContainerWrapper.EXTRA_SET_STACK = FunctionalStorageCompat::handleSetStack;
        SideContainerWrapper.FORCE_CLIENT_CACHE = h -> h instanceof BigInventoryHandler || h instanceof ControllerInventoryHandler;
    }

    private static boolean handleSetStack(IItemHandler handler, int slot, ItemStack stack) {
        if (!(handler instanceof BigInventoryHandler big)) return false;
        if (slot < 0 || slot >= big.getStoredStacks().size()) return true;
        BigInventoryHandler.BigStack bigStack = big.getStoredStacks().get(slot);
        if (stack.isEmpty()) {
            bigStack.setStack(ItemStack.EMPTY);
            bigStack.setAmount(0);
        } else {
            if (bigStack.getStack().isEmpty() || !ItemStack.isSameItemSameComponents(bigStack.getStack(), stack)) {
                bigStack.setStack(stack.copyWithCount(stack.getMaxStackSize()));
            }
            bigStack.setAmount(stack.getCount());
        }
        return true;
    }
}
