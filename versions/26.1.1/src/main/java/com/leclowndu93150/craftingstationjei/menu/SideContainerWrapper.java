package com.leclowndu93150.craftingstationjei.menu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class SideContainerWrapper {

    private final ResourceHandler<ItemResource> handler;

    public static final SideContainerWrapper EMPTY = new SideContainerWrapper(new ResourceHandler<>() {
        @Override public int size() { return 0; }
        @Override public ItemResource getResource(int index) { return ItemResource.EMPTY; }
        @Override public long getAmountAsLong(int index) { return 0; }
        @Override public long getCapacityAsLong(int index, ItemResource resource) { return 0; }
        @Override public boolean isValid(int index, ItemResource resource) { return false; }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) { return 0; }
        @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) { return 0; }
    });

    public SideContainerWrapper(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    public int getSlotCount() {
        return handler.size();
    }

    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= handler.size()) return ItemStack.EMPTY;
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        return resource.toStack(handler.getAmountAsInt(slot));
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= handler.size()) return;
        try (Transaction tx = Transaction.openRoot()) {
            ItemResource existing = handler.getResource(slot);
            if (!existing.isEmpty()) {
                handler.extract(slot, existing, handler.getAmountAsInt(slot), tx);
            }
            if (!stack.isEmpty()) {
                handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            }
            tx.commit();
        }
    }

    public ItemStack removeStack(int slot, int count) {
        if (slot < 0 || slot >= handler.size()) return ItemStack.EMPTY;
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, count, tx);
            tx.commit();
            if (extracted > 0) {
                return resource.toStack(extracted);
            }
        }
        return ItemStack.EMPTY;
    }

    public int getMaxStackSize(int slot) {
        if (slot < 0 || slot >= handler.size()) return 0;
        ItemResource resource = handler.getResource(slot);
        return handler.getCapacityAsInt(slot, resource.isEmpty() ? ItemResource.EMPTY : resource);
    }

    public boolean valid(int slot) {
        return slot >= 0 && slot < getSlotCount();
    }

    public ResourceHandler<ItemResource> getHandler() {
        return handler;
    }
}
