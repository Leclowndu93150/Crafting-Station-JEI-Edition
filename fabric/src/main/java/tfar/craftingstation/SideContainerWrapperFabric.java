package tfar.craftingstation;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import tfar.craftingstation.util.SideContainerWrapper;

public class SideContainerWrapperFabric implements SideContainerWrapper {
    private final InventoryStorage handler;

    public SideContainerWrapperFabric(InventoryStorage handler) {
        this.handler = handler;
    }

    @Override
    public int $getSlotCount() {
        return handler.getSlotCount();
    }

    @Override
    public ItemStack $getStack(int slot) {
        if (!$valid(slot)) return ItemStack.EMPTY;
        SingleSlotStorage<ItemVariant> singleSlotStorage = handler.getSlot(slot);

        ItemStack stack = singleSlotStorage.getResource().toStack((int) singleSlotStorage.getAmount());
        return stack;
    }

    @Override
    public void $setStack(int slot, ItemStack stack) {
        if ($valid(slot)) {
            SingleSlotStorage<ItemVariant> singleSlotStorage = handler.getSlot(slot);
            if (!singleSlotStorage.getResource().isBlank()) {
                Transaction transaction = Transaction.openNested(null);
                StorageUtil.extractAny(singleSlotStorage, Integer.MAX_VALUE, transaction);
                transaction.commit();
                //singleSlotStorage.extract(singleSlotStorage.getResource(),Integer.MAX_VALUE,Transaction.openNested(null));
            }
            if (!stack.isEmpty()) {
                  Transaction transaction = Transaction.openNested(null);
                  singleSlotStorage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                  transaction.commit();
            }
        }
    }

    @Override
    public ItemStack $removeStack(int slot, int count) {
        if (!$valid(slot))return ItemStack.EMPTY;
        SingleSlotStorage<ItemVariant> singleSlotStorage = handler.getSlot(slot);
        ItemStack stack = singleSlotStorage.getResource().toStack((int) singleSlotStorage.getAmount());
        ItemStack extract = stack.copyWithCount(Math.min(count,stack.getCount()));
        Transaction transaction = Transaction.openNested(null);
        StorageUtil.extractAny(singleSlotStorage,extract.getCount(),transaction);
        transaction.commit();
        return extract;
    }

    @Override
    public int $getMaxStackSize(int slot) {
        return 0;
    }

    @Override
    public ItemStack $insert(int slot, ItemStack stack, boolean simulate) {
        return null;
    }
}
