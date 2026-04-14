package com.leclowndu93150.craftingstationjei.menu;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.leclowndu93150.craftingstationjei.init.ModBlocks;
import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
import com.leclowndu93150.craftingstationjei.network.PacketHandler;
import com.leclowndu93150.craftingstationjei.network.S2CSideSetSideContainerSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
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
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import com.leclowndu93150.craftingstationjei.compat.PolymorphCompat;
import net.minecraftforge.fml.ModList;

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
    private final Map<Direction, Integer> clientSlotCountOverride = new EnumMap<>(Direction.class);
    private final Map<Direction, NonNullList<ItemStack>> clientDisplayCache = new EnumMap<>(Direction.class);

    public CraftingStationMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(9), buf.readBlockPos(), readSideSlotCounts(buf));
    }

    public CraftingStationMenu(int id, Inventory inv, CraftingStationBlockEntity blockEntity) {
        this(id, inv, blockEntity.getInput(), blockEntity.getBlockPos(), null);
    }

    public CraftingStationMenu(int id, Inventory inv, SimpleContainer simpleContainer, BlockPos pos) {
        this(id, inv, simpleContainer, pos, null);
    }

    public CraftingStationMenu(int id, Inventory inv, SimpleContainer simpleContainer, BlockPos pos,
                               Map<Direction, Integer> slotCountOverrides) {
        super(ModMenuTypes.CRAFTING_STATION.get(), id);
        this.player = inv.player;
        this.pos = pos;
        this.world = player.level();
        this.tileEntity = (CraftingStationBlockEntity) world.getBlockEntity(pos);
        this.craftMatrix = new PersistantCraftingContainer(this, simpleContainer);
        if (slotCountOverrides != null) {
            this.clientSlotCountOverride.putAll(slotCountOverrides);
        }

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

    public static void writeSideSlotCounts(FriendlyByteBuf buf, Level level, BlockPos pos) {
        Set<IItemHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Direction> dirs = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity te = level.getBlockEntity(neighbor);
            if (te == null || te instanceof CraftingStationBlockEntity) continue;
            var beType = ForgeRegistries.BLOCK_ENTITY_TYPES.getHolder(
                    ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(te.getType())).orElse(null);
            if (beType != null && beType.is(Craftingstationjei.BLACKLISTED)) continue;
            var cap = te.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (!cap.isPresent()) continue;
            IItemHandler handler = cap.orElseThrow(IllegalStateException::new);
            if (!seenHandlers.add(handler)) continue;
            dirs.add(dir);
            counts.add(handler.getSlots());
        }
        buf.writeVarInt(dirs.size());
        for (int i = 0; i < dirs.size(); i++) {
            buf.writeVarInt(dirs.get(i).get3DDataValue());
            buf.writeVarInt(counts.get(i));
        }
    }

    private static Map<Direction, Integer> readSideSlotCounts(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<Direction, Integer> out = new EnumMap<>(Direction.class);
        for (int i = 0; i < n; i++) {
            Direction dir = Direction.from3DDataValue(buf.readVarInt());
            int count = buf.readVarInt();
            out.put(dir, count);
        }
        return out;
    }

    private void searchSideInventories() {
        Set<IItemHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity te = world.getBlockEntity(neighbor);
            if (te == null || te instanceof CraftingStationBlockEntity) continue;

            var beType = ForgeRegistries.BLOCK_ENTITY_TYPES.getHolder(
                    ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(te.getType())).orElse(null);
            if (beType != null && beType.is(Craftingstationjei.BLACKLISTED)) continue;
            if (te instanceof Container container && !container.stillValid(player)) continue;

            var cap = te.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                IItemHandler handler = cap.orElseThrow(IllegalStateException::new);
                if (!seenHandlers.add(handler)) continue;
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
        // On client, the server may have reported additional (handler-zero-slots) side inventories
        // e.g. FunctionalStorage controllers whose ConnectedDrawers don't populate client-side.
        if (world.isClientSide) {
            for (Direction dir : clientSlotCountOverride.keySet()) {
                if (!blockEntityMap.containsKey(dir)) {
                    BlockPos neighbor = pos.relative(dir);
                    BlockEntity te = world.getBlockEntity(neighbor);
                    if (te == null) continue;
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
    }

    private void addSideInventorySlots() {
        sideContainerStartIndex = 10;
        visibleSideSlotCount = 0;
        if (!hasSideContainers()) return;

        if (sideSlots.isEmpty()) {
            for (Direction direction : Direction.values()) {
                BlockEntity blockEntity = blockEntityMap.get(direction);
                if (blockEntity == null) continue;
                int slotCount = getSlotCountFor(direction);
                for (int i = 0; i < slotCount; i++) {
                    SideContainerSlot slot = new SideContainerSlot(direction, i, HIDDEN_SLOT_POS, HIDDEN_SLOT_POS, this);
                    sideSlots.add(slot);
                    addSlot(slot);
                }
            }
        }
        refreshSideSlots();
    }

    private int getSlotCountFor(Direction direction) {
        Integer override = clientSlotCountOverride.get(direction);
        if (override != null) return override;
        SideContainerWrapper wrapper = getHandlerFor(direction);
        return wrapper != null ? wrapper.getSlotCount() : 0;
    }

    private boolean useClientCacheFor(Direction direction) {
        if (!world.isClientSide) return false;
        Integer override = clientSlotCountOverride.get(direction);
        if (override == null) return false;
        SideContainerWrapper wrapper = getHandlerFor(direction);
        return wrapper == null || wrapper.getSlotCount() < override;
    }

    private NonNullList<ItemStack> getClientCache(Direction direction, int size) {
        NonNullList<ItemStack> list = clientDisplayCache.get(direction);
        if (list == null || list.size() != size) {
            list = NonNullList.withSize(size, ItemStack.EMPTY);
            clientDisplayCache.put(direction, list);
        }
        return list;
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
        Direction selectedDir = getSelectedContainer();
        int totalSlots = selectedDir != null ? getSlotCountFor(selectedDir) : 0;

        int maxOffset = Math.max(0, totalSlots - VISIBLE_SLOTS);
        this.firstSlot = Mth.clamp(this.firstSlot, 0, maxOffset);

        int available = Math.max(0, totalSlots - this.firstSlot);
        visibleSideSlotCount = Math.min(available, VISIBLE_SLOTS);

        if (sideSlots.isEmpty()) return;

        boolean scrolling = totalSlots > VISIBLE_SLOTS;
        int xOffset = scrolling ? -125 : -117;

        for (int i = 0; i < sideSlots.size(); i++) {
            SideContainerSlot existing = sideSlots.get(i);
            Direction slotDirection = existing.getDirection();
            int actualSlot = existing.getActualSlot();

            int xPos = HIDDEN_SLOT_POS;
            int yPos = HIDDEN_SLOT_POS;

            if (slotDirection == selectedDir && actualSlot >= firstSlot && actualSlot < totalSlots) {
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
        Optional<CraftingRecipe> optional;
        if (ModList.get().isLoaded("polymorph")) {
            optional = PolymorphCompat.getRecipe(this, craftMatrix, world, player);
        } else {
            optional = world.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftMatrix, world);
        }
        if (optional.isPresent()) {
            CraftingRecipe recipe = optional.get();
            if (craftResult.setRecipeUsed(world, serverPlayer, recipe)) {
                ItemStack assembled = recipe.assemble(craftMatrix, world.registryAccess());
                if (assembled.isItemEnabled(world.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        craftResult.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    public SideContainerWrapper getCurrentHandler() {
        return getHandlerFor(getSelectedContainer());
    }

    public SideContainerWrapper getHandlerFor(Direction direction) {
        BlockEntity be = blockEntityMap.get(direction);
        if (be == null) return null;
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (cap.isPresent()) {
            return new SideContainerWrapper(cap.orElseThrow(IllegalStateException::new));
        }
        return null;
    }

    public boolean hasSideContainers() {
        return !blocks.isEmpty();
    }

    public int subContainerSize() {
        return currentContainer != null ? getSlotCountFor(currentContainer) : 0;
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
        this.lastSyncedStacks.clear();
        if (tileEntity != null && !world.isClientSide) {
            tileEntity.setCurrentContainer(dir);
        }
        refreshSideSlots();
    }

    public void setFirstSlot(int firstSlot) {
        if (currentContainer == null) {
            this.firstSlot = 0;
            return;
        }
        int totalSlots = getSlotCountFor(currentContainer);
        int maxOffset = Math.max(0, totalSlots - VISIBLE_SLOTS);
        int newFirst = Mth.clamp(firstSlot, 0, maxOffset);
        if (newFirst != this.firstSlot) {
            this.lastSyncedStacks.clear();
        }
        this.firstSlot = newFirst;
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
        Direction selected = getSelectedContainer();
        if (selected == null) return;
        SideContainerWrapper wrapper = getHandlerFor(selected);
        if (wrapper == null) return;
        int slotCount = wrapper.getSlotCount();
        NonNullList<ItemStack> lastSynced = lastSyncedStacks.computeIfAbsent(
                selected, d -> NonNullList.withSize(slotCount, ItemStack.EMPTY));
        if (lastSynced.size() != slotCount) {
            lastSynced = NonNullList.withSize(slotCount, ItemStack.EMPTY);
            lastSyncedStacks.put(selected, lastSynced);
        }
        int windowStart = firstSlot;
        int windowEnd = Math.min(slotCount, firstSlot + VISIBLE_SLOTS);
        for (int i = windowStart; i < windowEnd; i++) {
            ItemStack current = wrapper.getStack(i);
            ItemStack previous = lastSynced.get(i);
            if (!ItemStack.matches(current, previous)) {
                PacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                        new S2CSideSetSideContainerSlot(current, selected, i));
                lastSynced.set(i, current.copy());
            }
        }
    }

    public void handleSideSlotUpdate(ItemStack stack, Direction direction, int slot) {
        if (useClientCacheFor(direction)) {
            int size = getSlotCountFor(direction);
            if (slot < 0 || slot >= size) return;
            getClientCache(direction, size).set(slot, stack);
            return;
        }
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

    void flushAdjacentUpdate(Direction direction) {
        if (tileEntity == null) return;
        Level level = tileEntity.getLevel();
        if (level == null) return;
        BlockPos neighborPos = tileEntity.getBlockPos().relative(direction);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor == null) return;
        neighbor.setChanged();
        if (!level.isClientSide) {
            BlockState neighborState = neighbor.getBlockState();
            level.sendBlockUpdated(neighborPos, neighborState, neighborState, 3);
        }
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
            ItemStack raw;
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return ItemStack.EMPTY;
                raw = craftingStationMenu.getClientCache(direction, size).get(slotIndex);
            } else {
                SideContainerWrapper handler = getHandler();
                if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return ItemStack.EMPTY;
                raw = handler.getStack(slotIndex);
            }
            if (raw.isEmpty()) return ItemStack.EMPTY;
            int cap = raw.getMaxStackSize();
            if (raw.getCount() <= cap) return raw;
            return raw.copyWithCount(cap);
        }

        @Override
        public ItemStack remove(int amount) {
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return ItemStack.EMPTY;
                NonNullList<ItemStack> cache = craftingStationMenu.getClientCache(direction, size);
                ItemStack cur = cache.get(slotIndex);
                ItemStack taken = cur.copy();
                taken.setCount(Math.min(amount, cur.getCount()));
                cur.shrink(taken.getCount());
                return taken;
            }
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return ItemStack.EMPTY;
            ItemStack result = handler.removeStack(slotIndex, amount);
            craftingStationMenu.flushAdjacentUpdate(direction);
            return result;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                return slotIndex >= 0 && slotIndex < size;
            }
            SideContainerWrapper handler = getHandler();
            return craftingStationMenu.isValidSideSlot(handler, slotIndex) && handler.valid(slotIndex);
        }

        @Override
        public void set(ItemStack stack) {
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return;
                craftingStationMenu.getClientCache(direction, size).set(slotIndex, stack);
                return;
            }
            SideContainerWrapper handler = getHandler();
            if (craftingStationMenu.isValidSideSlot(handler, slotIndex)) {
                handler.setStack(slotIndex, stack);
                craftingStationMenu.flushAdjacentUpdate(direction);
            }
        }

        @Override
        public int getMaxStackSize() {
            ItemStack cur = getItem();
            return cur.isEmpty() ? 64 : cur.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return stack.getMaxStackSize();
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int count) {
            if (stack.isEmpty() || !mayPlace(stack)) return stack;
            int toInsert = Math.min(count, stack.getCount());
            toInsert = Math.min(toInsert, stack.getMaxStackSize());
            if (toInsert <= 0) return stack;

            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return stack;
                NonNullList<ItemStack> cache = craftingStationMenu.getClientCache(direction, size);
                ItemStack existing = cache.get(slotIndex);
                if (existing.isEmpty()) {
                    cache.set(slotIndex, stack.copyWithCount(toInsert));
                    stack.shrink(toInsert);
                } else if (ItemStack.isSameItemSameTags(existing, stack)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    int moved = Math.min(space, toInsert);
                    if (moved > 0) {
                        existing.grow(moved);
                        stack.shrink(moved);
                    }
                }
                return stack;
            }

            SideContainerWrapper handler = getHandler();
            if (handler == null) return stack;

            ItemStack attempt = stack.copyWithCount(toInsert);
            ItemStack leftover = handler.insert(slotIndex, attempt, false);
            int inserted = toInsert - leftover.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                craftingStationMenu.flushAdjacentUpdate(direction);
            }
            return stack;
        }

        public int getActualSlot() {
            return slotIndex;
        }

        public Direction getDirection() {
            return direction;
        }
    }
}
