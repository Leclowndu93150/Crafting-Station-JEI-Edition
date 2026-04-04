package com.leclowndu93150.craftingstationjei.menu;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.leclowndu93150.craftingstationjei.compat.PolymorphCompat;
import com.leclowndu93150.craftingstationjei.init.ModBlocks;
import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
import com.leclowndu93150.craftingstationjei.network.S2CSideSetSideContainerSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class CraftingStationMenu extends AbstractContainerMenu {

    private static final int HIDDEN_SLOT_POS = -2000;
    private static final int SLOTS_PER_ROW = 6;
    public static final int VISIBLE_SLOTS = 9 * SLOTS_PER_ROW;

    public final PersistantCraftingContainer craftMatrix;
    public final ResultContainer craftResult = new ResultContainer();
    public final Level world;
    public final CraftingStationBlockEntity tileEntity;
    private final Player player;
    private final BlockPos pos;

    public final Map<Direction, ItemStack> blocks = new EnumMap<>(Direction.class);
    public final Map<Direction, BlockEntity> blockEntityMap = new EnumMap<>(Direction.class);
    public final Map<Direction, Component> containerNames = new EnumMap<>(Direction.class);

    private Direction currentContainer = null;
    private int firstSlot;
    private int visibleSideSlotCount;
    private int sideContainerStartIndex;
    private int playerInventoryStartIndex;

    private final List<SideContainerSlot> sideSlots = new ArrayList<>();
    private final Map<Direction, NonNullList<ItemStack>> lastSyncedStacks = new HashMap<>();

    public CraftingStationMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(9), buf.readBlockPos());
    }

    public CraftingStationMenu(int id, Inventory inv, CraftingStationBlockEntity blockEntity) {
        this(id, inv, blockEntity.getInput(), blockEntity.getBlockPos());
    }

    public CraftingStationMenu(int id, Inventory inv, SimpleContainer simpleContainer, BlockPos pos) {
        super(ModMenuTypes.CRAFTING_STATION.get(), id);
        this.player = inv.player;
        this.pos = pos;
        this.world = player.level();
        this.tileEntity = (CraftingStationBlockEntity) world.getBlockEntity(pos);
        this.craftMatrix = new PersistantCraftingContainer(this, simpleContainer);

        addOwnSlots();
        searchSideInventories();
        if (tileEntity != null && tileEntity.getCurrentContainer() != null
                && blockEntityMap.containsKey(tileEntity.getCurrentContainer())) {
            this.currentContainer = tileEntity.getCurrentContainer();
        } else if (!blockEntityMap.isEmpty()) {
            this.currentContainer = blockEntityMap.keySet().iterator().next();
        }
        addSideInventorySlots();
        addPlayerSlots(inv);
        slotsChanged(craftMatrix);
    }

    private void addOwnSlots() {
        addSlot(new ResultSlot(player, craftMatrix, craftResult, 0, 124, 35));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new Slot(craftMatrix, x + 3 * y, 30 + 18 * x, 17 + 18 * y));
            }
        }
    }

    private void searchSideInventories() {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity te = world.getBlockEntity(neighbor);
            if (te == null || te instanceof CraftingStationBlockEntity) continue;

            var beType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getHolder(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(te.getType())).orElse(null);
            if (beType != null && beType.is(Craftingstationjei.BLACKLISTED)) continue;
            if (te instanceof Container container && !container.stillValid(player)) continue;

            IItemHandler handler = world.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, null);
            if (handler != null) {
                blockEntityMap.put(dir, te);
                BlockState neighborState = world.getBlockState(neighbor);
                ItemStack displayStack = neighborState.getBlock().getCloneItemStack(world, neighbor, neighborState);
                if (displayStack.isEmpty()) {
                    displayStack = new ItemStack(neighborState.getBlock());
                }
                blocks.put(dir, displayStack);
                containerNames.put(dir, te instanceof net.minecraft.world.Nameable nameable
                        ? nameable.getDisplayName() : Component.translatable(neighborState.getBlock().getDescriptionId()));
            }
        }
    }

    private void addSideInventorySlots() {
        sideContainerStartIndex = 10;
        visibleSideSlotCount = 0;
        if (!hasSideContainers()) return;

        if (sideSlots.isEmpty()) {
            for (Direction direction : Direction.values()) {
                BlockEntity blockEntity = blockEntityMap.get(direction);
                if (blockEntity == null) continue;
                SideContainerWrapper wrapper = getHandlerFor(direction);
                if (wrapper == null) continue;
                int slotCount = wrapper.getSlotCount();
                for (int i = 0; i < slotCount; i++) {
                    SideContainerSlot slot = new SideContainerSlot(direction, i, HIDDEN_SLOT_POS, HIDDEN_SLOT_POS, this);
                    sideSlots.add(slot);
                    addSlot(slot);
                }
            }
        }
        refreshSideSlots();
    }

    private void addPlayerSlots(Inventory playerInventory) {
        playerInventoryStartIndex = sideContainerStartIndex + sideSlots.size();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(playerInventory, 9 + x + 9 * y, 8 + 18 * x, 84 + 18 * y));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(playerInventory, x, 8 + 18 * x, 142));
        }
    }

    public void refreshSideSlots() {
        SideContainerWrapper handler = getCurrentHandler();
        int totalSlots = handler != null ? handler.getSlotCount() : 0;

        int maxOffset = Math.max(0, totalSlots - VISIBLE_SLOTS);
        this.firstSlot = Mth.clamp(this.firstSlot, 0, maxOffset);

        int available = Math.max(0, totalSlots - this.firstSlot);
        visibleSideSlotCount = Math.min(available, VISIBLE_SLOTS);

        if (sideSlots.isEmpty()) return;

        boolean scrolling = totalSlots > VISIBLE_SLOTS;
        int xOffset = scrolling ? -125 : -117;
        Direction selected = getSelectedContainer();

        for (int i = 0; i < sideSlots.size(); i++) {
            SideContainerSlot existing = sideSlots.get(i);
            Direction slotDirection = existing.getDirection();
            int actualSlot = existing.getActualSlot();

            int xPos = HIDDEN_SLOT_POS;
            int yPos = HIDDEN_SLOT_POS;

            if (slotDirection == selected && actualSlot >= firstSlot && actualSlot < totalSlots) {
                int displayIndex = actualSlot - firstSlot;
                if (displayIndex >= 0 && displayIndex < visibleSideSlotCount) {
                    int row = displayIndex / SLOTS_PER_ROW;
                    int col = displayIndex % SLOTS_PER_ROW;
                    xPos = xOffset + col * 18;
                    yPos = 17 + row * 18;
                }
            }

            if (existing.x != xPos || existing.y != yPos) {
                SideContainerSlot replacement = new SideContainerSlot(slotDirection, actualSlot, xPos, yPos, this);
                replacement.index = existing.index;
                sideSlots.set(i, replacement);
                int slotListIndex = sideContainerStartIndex + i;
                if (slotListIndex < this.slots.size()) {
                    this.slots.set(slotListIndex, replacement);
                }
            }
        }
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory == craftMatrix) {
            craftMatrix.setDoNotCallUpdates(true);
            try {
                slotChangedCraftingGrid();
            } finally {
                craftMatrix.setDoNotCallUpdates(false);
            }
        }
    }

    private void slotChangedCraftingGrid() {
        if (world.isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        CraftingInput craftInput = craftMatrix.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> optional;
        if (ModList.get().isLoaded("polymorph")) {
            optional = PolymorphCompat.getRecipe(this, craftMatrix, world, player);
        } else {
            optional = world.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftInput, world);
        }
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            CraftingRecipe recipe = holder.value();
            if (craftResult.setRecipeUsed(world, serverPlayer, holder)) {
                ItemStack assembled = recipe.assemble(craftInput, world.registryAccess());
                if (assembled.isItemEnabled(world.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        craftResult.setItem(0, result);
        setRemoteSlot(0, result);
        broadcastFullState();
    }

    public SideContainerWrapper getCurrentHandler() {
        return getHandlerFor(getSelectedContainer());
    }

    public SideContainerWrapper getHandlerFor(Direction direction) {
        BlockEntity be = blockEntityMap.get(direction);
        if (be == null) return null;
        IItemHandler handler = world.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), null);
        if (handler != null) {
            return new SideContainerWrapper(handler);
        }
        return null;
    }

    public boolean hasSideContainers() {
        return !blocks.isEmpty();
    }

    public int subContainerSize() {
        SideContainerWrapper handler = getCurrentHandler();
        return handler != null ? handler.getSlotCount() : 0;
    }

    public boolean needsScroll() {
        return subContainerSize() > VISIBLE_SLOTS;
    }

    public Direction getSelectedContainer() {
        return currentContainer;
    }

    public void setCurrentContainer(Direction dir) {
        this.currentContainer = dir;
        this.firstSlot = 0;
        if (tileEntity != null && !world.isClientSide) {
            tileEntity.setCurrentContainer(dir);
        }
        refreshSideSlots();
    }

    public void setFirstSlot(int firstSlot) {
        SideContainerWrapper handler = getCurrentHandler();
        if (handler == null) {
            this.firstSlot = 0;
            return;
        }
        int maxOffset = Math.max(0, handler.getSlotCount() - VISIBLE_SLOTS);
        this.firstSlot = Mth.clamp(firstSlot, 0, maxOffset);
        refreshSideSlots();
    }

    public int getFirstSlot() {
        return firstSlot;
    }

    public int getVisibleSideSlotCount() {
        return visibleSideSlotCount;
    }

    public enum ButtonAction {
        CLEAR, TAB_0, TAB_1, TAB_2, TAB_3, TAB_4, TAB_5;
        static final ButtonAction[] VALUES = values();
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int id) {
        if (id < 0 || id >= ButtonAction.VALUES.length) return false;
        ButtonAction action = ButtonAction.VALUES[id];
        if (pPlayer instanceof ServerPlayer) {
            switch (action) {
                case CLEAR -> {
                    for (int i = 1; i < 10; i++) quickMoveStack(player, i);
                }
                case TAB_0, TAB_1, TAB_2, TAB_3, TAB_4, TAB_5 -> {
                    Direction direction = Direction.values()[id - 1];
                    if (blockEntityMap.get(direction) != null)
                        setCurrentContainer(direction);
                }
            }
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && tileEntity != null) {
            tileEntity.setCurrentContainer(currentContainer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack slotStack = slot.getItem();
        copy = slotStack.copy();

        if (index == 0) {
            slotStack.getItem().onCraftedBy(slotStack, world, player);
            if (!moveItemStackTo(slotStack, playerInventoryStartIndex, playerInventoryStartIndex + 36, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(slotStack, copy);
        } else if (index >= 1 && index <= 9) {
            if (!moveItemStackTo(slotStack, playerInventoryStartIndex, playerInventoryStartIndex + 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= sideContainerStartIndex && index < playerInventoryStartIndex) {
            if (!moveItemStackTo(slotStack, playerInventoryStartIndex, playerInventoryStartIndex + 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInventoryStartIndex) {
            if (!moveItemStackTo(slotStack, 1, 10, false)) {
                if (index < playerInventoryStartIndex + 27) {
                    if (!moveItemStackTo(slotStack, playerInventoryStartIndex + 27, playerInventoryStartIndex + 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!moveItemStackTo(slotStack, playerInventoryStartIndex, playerInventoryStartIndex + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        if (index == 0) {
            player.drop(slotStack, false);
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockState state = player.level().getBlockState(pos);
        return (state.is(ModBlocks.CRAFTING_STATION.get()) || state.is(ModBlocks.CRAFTING_STATION_SLAB.get()))
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (hasSideContainers()) {
            syncSideContainers();
        }
    }

    private void syncSideContainers() {
        for (Map.Entry<Direction, BlockEntity> entry : blockEntityMap.entrySet()) {
            Direction direction = entry.getKey();
            SideContainerWrapper wrapper = getHandlerFor(direction);
            if (wrapper == null) continue;
            int slotCount = wrapper.getSlotCount();
            NonNullList<ItemStack> lastSynced = lastSyncedStacks.computeIfAbsent(
                    direction, d -> NonNullList.withSize(slotCount, ItemStack.EMPTY));
            if (lastSynced.size() != slotCount) {
                lastSynced = NonNullList.withSize(slotCount, ItemStack.EMPTY);
                lastSyncedStacks.put(direction, lastSynced);
            }
            for (int i = 0; i < slotCount; i++) {
                ItemStack current = wrapper.getStack(i);
                ItemStack previous = lastSynced.get(i);
                if (!ItemStack.matches(current, previous)) {
                    PacketDistributor.sendToPlayer((ServerPlayer) player,
                            new S2CSideSetSideContainerSlot(current, direction, i));
                    lastSynced.set(i, current.copy());
                }
            }
        }
    }

    public void handleSideSlotUpdate(ItemStack stack, Direction direction, int slot) {
        SideContainerWrapper wrapper = getHandlerFor(direction);
        if (wrapper != null && direction != getSelectedContainer()) {
            wrapper.setStack(slot, stack);
        }
    }

    public int getSideContainerStartIndex() {
        return sideContainerStartIndex;
    }

    public int getPlayerInventoryStartIndex() {
        return playerInventoryStartIndex;
    }

    public List<SideContainerSlot> getSideSlots() {
        return sideSlots;
    }

    private boolean isValidSideSlot(SideContainerWrapper handler, int slot) {
        return handler != null && slot >= 0 && slot < handler.getSlotCount();
    }

    public static class SideContainerSlot extends Slot {
        private final CraftingStationMenu craftingStationMenu;
        private final Direction direction;
        private final int slotIndex;

        public SideContainerSlot(Direction direction, int slotIndex, int x, int y, CraftingStationMenu menu) {
            super(new SimpleContainer(0), slotIndex, x, y);
            this.craftingStationMenu = menu;
            this.direction = direction;
            this.slotIndex = slotIndex;
        }

        private SideContainerWrapper getHandler() {
            return craftingStationMenu.getHandlerFor(direction);
        }

        @Override
        public ItemStack getItem() {
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return ItemStack.EMPTY;
            return handler.getStack(slotIndex);
        }

        @Override
        public ItemStack remove(int amount) {
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return ItemStack.EMPTY;
            return handler.removeStack(slotIndex, amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            SideContainerWrapper handler = getHandler();
            return craftingStationMenu.isValidSideSlot(handler, slotIndex) && handler.valid(slotIndex);
        }

        @Override
        public void set(ItemStack stack) {
            SideContainerWrapper handler = getHandler();
            if (craftingStationMenu.isValidSideSlot(handler, slotIndex)) {
                handler.setStack(slotIndex, stack);
            }
        }

        @Override
        public int getMaxStackSize() {
            SideContainerWrapper handler = getHandler();
            return craftingStationMenu.isValidSideSlot(handler, slotIndex) ? handler.getMaxStackSize(slotIndex) : 0;
        }

        public int getActualSlot() {
            return slotIndex;
        }

        public Direction getDirection() {
            return direction;
        }
    }
}
