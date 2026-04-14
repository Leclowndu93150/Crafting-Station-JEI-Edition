package com.leclowndu93150.craftingstationjei.compat;

import com.buuz135.functionalstorage.inventory.BigInventoryHandler;
import com.leclowndu93150.craftingstationjei.menu.SideContainerWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class FunctionalStorageCompat {

    public static void init() {
        SideContainerWrapper.EXTRA_SET_STACK = FunctionalStorageCompat::handleSetStack;
    }

    private static boolean handleSetStack(IItemHandler handler, int slot, ItemStack stack) {
        if (!(handler instanceof BigInventoryHandler)) return false;
        ItemStack cached = handler.getStackInSlot(slot);
        if (!cached.isEmpty()) {
            cached.setCount(stack.getCount());
        }
        return true;
    }
}
