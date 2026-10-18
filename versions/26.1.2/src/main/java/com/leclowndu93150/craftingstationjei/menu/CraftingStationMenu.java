package com.leclowndu93150.craftingstationjei.menu;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.leclowndu93150.craftingstationjei.init.ModBlocks;
import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
import com.leclowndu93150.craftingstationjei.network.S2CSideSetSideContainerSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

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
    private final boolean serverDefinedSides;

    public CraftingStationMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
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
        this.serverDefinedSides = slotCountOverrides != null;
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

    public static void writeSideSlotCounts(RegistryFriendlyByteBuf buf, Level level, BlockPos pos, Player player) {
        Map<Direction, ResourceHandler<ItemResource>> handlers = findSideHandlers(level, pos, player);
        buf.writeVarInt(handlers.size());
        handlers.forEach((dir, handler) -> {
            buf.writeVarInt(dir.get3DDataValue());
            buf.writeVarInt(handler.size());
        });
    }

    private static Map<Direction, Integer> readSideSlotCounts(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<Direction, Integer> out = new EnumMap<>(Direction.class);
        for (int i = 0; i < n; i++) {
            Direction dir = Direction.from3DDataValue(buf.readVarInt());
            int count = buf.readVarInt();
            out.put(dir, count);
        }
        return out;
    }

    private static Map<Direction, ResourceHandler<ItemResource>> findSideHandlers(Level level, BlockPos pos, Player player) {
        Map<Direction, ResourceHandler<ItemResource>> found = new EnumMap<>(Direction.class);
        Set<ResourceHandler<ItemResource>> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity te = level.getBlockEntity(neighbor);
            if (te == null || te instanceof CraftingStationBlockEntity) continue;
            if (te.typeHolder().is(Craftingstationjei.BLACKLISTED)) continue;
            if (te instanceof Container container && !container.stillValid(player)) continue;
            ResourceHandler<ItemResource> handler = getUsableHandler(level, neighbor);
            if (handler == null || !seenHandlers.add(handler)) continue;
            found.put(dir, handler);
        }
        return found;
    }

    private void searchSideInventories() {
        if (world.isClientSide() && serverDefinedSides) {
            for (Direction dir : clientSlotCountOverride.keySet()) {
                BlockEntity te = world.getBlockEntity(pos.relative(dir));
                if (te != null) addSideInventory(dir, te);
            }
            return;
        }
        for (Direction dir : findSideHandlers(world, pos, player).keySet()) {
            addSideInventory(dir, world.getBlockEntity(pos.relative(dir)));
        }
    }

    private void addSideInventory(Direction dir, BlockEntity te) {
        BlockState neighborState = world.getBlockState(te.getBlockPos());
        blockEntityMap.put(dir, te);
        blocks.put(dir, new ItemStack(neighborState.getBlock()));
        containerNames.put(dir, te instanceof Nameable nameable
                ? nameable.getDisplayName() : Component.translatable(neighborState.getBlock().getDescriptionId()));
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
        if (!world.isClientSide()) return false;
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

    public int getMaxFirstSlot() {
        int totalSlots = subContainerSize();
        int rows = (totalSlots + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
        return Math.max(0, rows - VISIBLE_SLOTS / SLOTS_PER_ROW) * SLOTS_PER_ROW;
    }

    public void refreshSideSlots() {
        int totalSlots = subContainerSize();

        this.firstSlot = Mth.clamp(this.firstSlot, 0, getMaxFirstSlot());

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
        if (world.isClientSide()) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        CraftingInput craftInput = craftMatrix.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> optional;
        // TODO: Re-enable when Polymorph is available for 26.1
        // if (ModList.get().isLoaded("polymorph")) {
        //     optional = PolymorphCompat.getRecipe(this, craftMatrix, world, player);
        // } else {
            optional = world.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftInput, world);
        // }
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            CraftingRecipe recipe = holder.value();
            if (craftResult.setRecipeUsed(serverPlayer, holder)) {
                ItemStack assembled = recipe.assemble(craftInput);
                if (assembled.isItemEnabled(world.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        craftResult.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    public SideContainerWrapper getCurrentHandler() {
        return getHandlerFor(getSelectedContainer());
    }

    public SideContainerWrapper getHandlerFor(Direction direction) {
        BlockEntity be = blockEntityMap.get(direction);
        if (be == null) return null;
        ResourceHandler<ItemResource> handler = getUsableHandler(world, be.getBlockPos());
        if (handler != null) {
            return new SideContainerWrapper(handler);
        }
        return null;
    }

    private static ResourceHandler<ItemResource> getUsableHandler(Level level, BlockPos pos) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (handler != null) return handler;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container chest = ChestBlock.getContainer(chestBlock, state, level, pos, true);
            if (chest != null) return VanillaContainerWrapper.of(chest);
        }
        return level.getBlockEntity(pos) instanceof Container container ? VanillaContainerWrapper.of(container) : null;
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
        if (tileEntity != null && !world.isClientSide()) {
            tileEntity.setCurrentContainer(dir);
        }
        refreshSideSlots();
    }

    public void setFirstSlot(int firstSlot) {
        if (currentContainer == null) {
            this.firstSlot = 0;
            return;
        }
        this.firstSlot = Mth.clamp(firstSlot - Math.floorMod(firstSlot, SLOTS_PER_ROW), 0, getMaxFirstSlot());
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
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (input == ContainerInput.SWAP && (button >= 0 && button < 9 || button == 40)
                && slotId >= 0 && slotId < slots.size()
                && slots.get(slotId) instanceof SideContainerSlot sideSlot) {
            swapWithSideSlot(sideSlot, button, player);
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    private void swapWithSideSlot(SideContainerSlot sideSlot, int button, Player player) {
        Inventory inventory = player.getInventory();
        ItemStack source = inventory.getItem(button);
        ItemStack target = sideSlot.getItem();
        if (source.isEmpty()) {
            if (!target.isEmpty()) {
                inventory.setItem(button, sideSlot.safeTake(Math.min(target.getCount(), target.getMaxStackSize()), Integer.MAX_VALUE, player));
            }
        } else if (target.isEmpty()) {
            inventory.setItem(button, sideSlot.safeInsert(source));
        }
    }

    private ItemStack sideQuickMove(Player player, SideContainerSlot sideSlot) {
        Direction dir = sideSlot.getDirection();
        int slotIdx = sideSlot.getActualSlot();

        if (useClientCacheFor(dir)) {
            int size = getSlotCountFor(dir);
            if (slotIdx < 0 || slotIdx >= size) return ItemStack.EMPTY;
            NonNullList<ItemStack> cache = getClientCache(dir, size);
            ItemStack cur = cache.get(slotIdx);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            int take = Math.min(cur.getCount(), cur.getMaxStackSize());
            ItemStack moved = cur.copyWithCount(take);
            if (!moveItemStackTo(moved, playerInventoryStartIndex, playerInventoryStartIndex + 36, false)) {
                return ItemStack.EMPTY;
            }
            int actuallyMoved = take - moved.getCount();
            if (actuallyMoved <= 0) return ItemStack.EMPTY;
            cur.shrink(actuallyMoved);
            return cur.copyWithCount(actuallyMoved);
        }

        SideContainerWrapper wrapper = getHandlerFor(dir);
        if (wrapper == null) return ItemStack.EMPTY;

        ItemStack peek = wrapper.getStack(slotIdx);
        if (peek.isEmpty()) return ItemStack.EMPTY;
        int take = Math.min(peek.getCount(), peek.getMaxStackSize());
        ItemStack pulled = wrapper.removeStack(slotIdx, take);
        if (pulled.isEmpty()) return ItemStack.EMPTY;

        int beforeCount = pulled.getCount();
        boolean movedAny = moveItemStackTo(pulled, playerInventoryStartIndex, playerInventoryStartIndex + 36, false);
        if (!pulled.isEmpty()) {
            ItemStack leftover = wrapper.insert(slotIdx, pulled, false);
            if (!leftover.isEmpty()) {
                player.drop(leftover, false);
            }
        }
        if (!movedAny) return ItemStack.EMPTY;
        return peek.copyWithCount(beforeCount - pulled.getCount());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && tileEntity != null) {
            tileEntity.setCurrentContainer(currentContainer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        if (slot instanceof SideContainerSlot sideSlot) {
            return sideQuickMove(player, sideSlot);
        }

        ItemStack slotStack = slot.getItem();
        copy = slotStack.copy();

        if (index == 0) {
            slotStack.getItem().onCraftedBy(slotStack, player);
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
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return ItemStack.EMPTY;
                return craftingStationMenu.getClientCache(direction, size).get(slotIndex);
            }
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return ItemStack.EMPTY;
            return handler.getStack(slotIndex);
        }

        @Override
        public ItemStack remove(int amount) {
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return ItemStack.EMPTY;
                NonNullList<ItemStack> cache = craftingStationMenu.getClientCache(direction, size);
                ItemStack cur = cache.get(slotIndex);
                ItemStack taken = cur.copy();
                taken.setCount(Math.min(Math.min(amount, cur.getMaxStackSize()), cur.getCount()));
                cur.shrink(taken.getCount());
                return taken;
            }
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return ItemStack.EMPTY;
            ItemStack existing = handler.getStack(slotIndex);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            return handler.removeStack(slotIndex, Math.min(amount, existing.getMaxStackSize()));
        }

        @Override
        public boolean mayPickup(Player player) {
            if (craftingStationMenu.world.isClientSide()) return true;
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return false;
            return handler.getStack(slotIndex).isEmpty() || handler.canExtract(slotIndex);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return false;
                ItemStack existing = craftingStationMenu.getClientCache(direction, size).get(slotIndex);
                return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack);
            }
            SideContainerWrapper handler = getHandler();
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex) || !handler.valid(slotIndex)) return false;
            ItemStack existing = handler.getStack(slotIndex);
            return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack);
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
            if (!craftingStationMenu.isValidSideSlot(handler, slotIndex)) return;
            if (!handler.setStack(slotIndex, stack)) {
                applyDelta(handler, stack);
            }
        }

        private void applyDelta(SideContainerWrapper handler, ItemStack target) {
            ItemStack existing = handler.getStack(slotIndex);
            if (!existing.isEmpty() && (target.isEmpty() || !ItemStack.isSameItemSameComponents(existing, target))) {
                while (!existing.isEmpty() && !handler.removeStack(slotIndex, existing.getCount()).isEmpty()) {
                    existing = handler.getStack(slotIndex);
                }
            }
            if (target.isEmpty()) return;
            int delta = target.getCount() - handler.getStack(slotIndex).getCount();
            if (delta > 0) {
                handler.insert(slotIndex, target.copyWithCount(delta), false);
            } else if (delta < 0) {
                handler.removeStack(slotIndex, -delta);
            }
        }

        @Override
        public int getMaxStackSize() {
            if (craftingStationMenu.useClientCacheFor(direction)) return Item.ABSOLUTE_MAX_STACK_SIZE;
            SideContainerWrapper handler = getHandler();
            return craftingStationMenu.isValidSideSlot(handler, slotIndex) ? handler.getMaxStackSize(slotIndex) : 0;
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int count) {
            if (stack.isEmpty() || !mayPlace(stack)) return stack;
            int toInsert = Math.min(Math.min(count, stack.getCount()), stack.getMaxStackSize());
            if (toInsert <= 0) return stack;

            if (craftingStationMenu.useClientCacheFor(direction)) {
                int size = craftingStationMenu.getSlotCountFor(direction);
                if (slotIndex < 0 || slotIndex >= size) return stack;
                NonNullList<ItemStack> cache = craftingStationMenu.getClientCache(direction, size);
                ItemStack existing = cache.get(slotIndex);
                if (existing.isEmpty()) {
                    cache.set(slotIndex, stack.copyWithCount(toInsert));
                    stack.shrink(toInsert);
                } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), toInsert);
                    if (moved > 0) {
                        existing.grow(moved);
                        stack.shrink(moved);
                    }
                }
                return stack;
            }

            SideContainerWrapper handler = getHandler();
            if (handler == null) return stack;
            ItemStack leftover = handler.insert(slotIndex, stack.copyWithCount(toInsert), false);
            stack.shrink(toInsert - leftover.getCount());
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
