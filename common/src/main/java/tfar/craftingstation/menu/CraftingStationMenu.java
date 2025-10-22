package tfar.craftingstation.menu;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.*;
import tfar.craftingstation.CommonTagUtil;
import tfar.craftingstation.CraftingStation;
import tfar.craftingstation.ModIntegration;
import tfar.craftingstation.PersistantCraftingContainer;
import tfar.craftingstation.blockentity.CraftingStationBlockEntity;
import tfar.craftingstation.init.ModMenuTypes;
import tfar.craftingstation.network.S2CSideSetSideContainerSlot;
import tfar.craftingstation.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tfar.craftingstation.util.SideContainerWrapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static tfar.craftingstation.client.CraftingStationScreen.VISIBLE_SLOTS;

public class CraftingStationMenu extends AbstractContainerMenu {

    private static final int HIDDEN_SLOT_POS = -2000;
    private static final int SLOTS_PER_ROW = 6;

    public final PersistantCraftingContainer craftMatrix;
    public final ResultContainer craftResult = new ResultContainer();
    public final Level world;
    public final CraftingStationBlockEntity tileEntity;
    private int sideContainerStartIndex;
    private int playerInventoryStartIndex;

    public Map<Direction, ItemStack> blocks = new EnumMap<>(Direction.class);
    public Map<Direction, BlockEntity> blockEntityMap = new EnumMap<>(Direction.class);

    public final Map<Direction, Component> containerNames = new EnumMap<>(Direction.class);
    private final Player player;
    private final BlockPos pos;
    private int firstSlot;
    private int visibleSideSlotCount;
    private final List<SideContainerSlot> sideSlots = new ArrayList<>(VISIBLE_SLOTS);

    public CraftingStationMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, new SimpleContainer(9), pos);
    }


    public CraftingStationMenu(int id, Inventory inv, SimpleContainer simpleContainer, BlockPos pos) {
        super(ModMenuTypes.crafting_station, id);
        this.player = inv.player;
        this.pos = pos;
        this.world = player.level();
        this.tileEntity = (CraftingStationBlockEntity) ModIntegration.getTileEntityAtPos(player.level(), pos);
        setCurrentContainer(tileEntity.getCurrentContainer());
        this.craftMatrix = new PersistantCraftingContainer(this, simpleContainer);


        addOwnSlots();

        if (Services.PLATFORM.getConfig().sideContainers()) {
            searchSideInventories();
        }

        addSideInventorySlots();
        addPlayerSlots(inv);
        slotsChanged(craftMatrix);
    }

    public static class SideContainerSlot extends Slot {
        private final CraftingStationMenu craftingStationMenu;
        private final int slotIndex;

        public SideContainerSlot(int slot, int x, int y, CraftingStationMenu craftingStationMenu) {
            super(new SimpleContainer(0), slot, x, y);
            this.craftingStationMenu = craftingStationMenu;
            this.slotIndex = slot;
        }

        @Override
        public ItemStack getItem() {
            SideContainerWrapper handler = craftingStationMenu.getCurrentHandler();
            int actualSlot = getActualSlot();
            if (!craftingStationMenu.isValidSideSlot(handler, actualSlot)) {
                return ItemStack.EMPTY;
            }
            return handler.$getStack(actualSlot);
        }

        @Override
        public ItemStack remove(int amount) {
            SideContainerWrapper handler = craftingStationMenu.getCurrentHandler();
            int actualSlot = getActualSlot();
            if (!craftingStationMenu.isValidSideSlot(handler, actualSlot)) {
                return ItemStack.EMPTY;
            }
            return handler.$removeStack(actualSlot, amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            SideContainerWrapper handler = craftingStationMenu.getCurrentHandler();
            int actualSlot = getActualSlot();
            return craftingStationMenu.isValidSideSlot(handler, actualSlot) && handler.$valid(actualSlot);
        }

        @Override
        public void set(ItemStack stack) {
            SideContainerWrapper handler = craftingStationMenu.getCurrentHandler();
            int actualSlot = getActualSlot();
            if (craftingStationMenu.isValidSideSlot(handler, actualSlot)) {
                handler.$setStack(actualSlot, stack);
            }
        }

        @Override
        public int getMaxStackSize() {
            SideContainerWrapper handler = craftingStationMenu.getCurrentHandler();
            int actualSlot = getActualSlot();
            return craftingStationMenu.isValidSideSlot(handler, actualSlot) ? handler.$getMaxStackSize(actualSlot) : 0;
        }

        public int getActualSlot() {
            return slotIndex + craftingStationMenu.getFirstSlot();
        }

        public int getSlotIndex() {
            return slotIndex;
        }
    }


    public SideContainerWrapper getCurrentHandler() {
        return Services.PLATFORM.getWrapper(blockEntityMap.get(getSelectedContainer()));
    }

    protected void addSideInventorySlots() {
        setSideContainerStartIndex(10);
        visibleSideSlotCount = 0;

        if (!hasSideContainers()) {
            return;
        }

        if (!sideSlots.isEmpty()) {
            refreshSideSlots();
            return;
        }

        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            SideContainerSlot slot = new SideContainerSlot(i, HIDDEN_SLOT_POS, HIDDEN_SLOT_POS, this);
            sideSlots.add(slot);
            addSlot(slot);
        }

        refreshSideSlots();
    }

    public boolean hasSideContainers() {
        return !blocks.isEmpty();
    }

    public int subContainerSize() {
        SideContainerWrapper handler = getCurrentHandler();
        return handler != null ? handler.$getSlotCount() : 0;
    }


    public Direction getSelectedContainer() {
        return currentContainer;
    }

    //it goes crafting output slot | 0
    //crafting input slots | 1 to 9
    //side inventories (if any) | 10 to (9 + subContainerSize)
    //player inventory | (10 + subContainerSides)

    public void searchSideInventories() {
        Direction defaultDirection = null;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity te = world.getBlockEntity(neighbor);

            if (te != null && !(te instanceof CraftingStationBlockEntity)) {
                if (CommonTagUtil.isIn(CraftingStation.blacklisted, te.getType())) continue;
                if (te instanceof Container container && !container.stillValid(player)) continue;

                if (Services.PLATFORM.hasCapability(te)) {
                    blockEntityMap.put(dir, te);
                    blocks.put(dir, new ItemStack(world.getBlockState(neighbor).getBlock()));
                    containerNames.put(dir, te instanceof MenuProvider menuProvider ? menuProvider.getDisplayName() : te.getBlockState().getBlock().getName());

                    if (defaultDirection == null && currentContainer == Direction.DOWN) {
                        defaultDirection = dir;
                    }
                }
            }
        }

        if (defaultDirection != null) {
            currentContainer = defaultDirection;
        }
    }


    private void addOwnSlots() {
        // crafting result
        this.addSlot(new ResultSlot(player, this.craftMatrix, craftResult, 0, 124, 35));

        // crafting grid
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new Slot(craftMatrix, x + 3 * y, 30 + 18 * x, 17 + 18 * y));
            }
        }
    }

    protected void addPlayerSlots(Inventory playerInventory) {
        setPlayerInventoryStartIndex(getSideContainerStartIndex(Direction.NORTH) + sideSlots.size());

        // Player Inventory (3 rows of 9 slots)
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(playerInventory, 9 + x + 9 * y, 8 + 18 * x, 84 + 18 * y));
            }
        }

        // Player Hotbar (1 row of 9 slots)
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(playerInventory, x, 8 + 18 * x, 142));
        }
    }

    // update crafting
    //clientside only
    //@Override
    //public void setAll(List<ItemStack> p_190896_1_) {
    //    craftMatrix.setDoNotCallUpdates(true);
    //    super.setAll(p_190896_1_);
    //   craftMatrix.setDoNotCallUpdates(false);
    //   craftMatrix.onCraftMatrixChanged();
    // }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory == craftMatrix) {
            craftMatrix.setDoNotCallUpdates(true);
            try {
                slotChangedCraftingGrid(this, world, player, craftMatrix, craftResult, null);
            } finally {
                craftMatrix.setDoNotCallUpdates(false);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        if (hasSideContainers()) {
            return handleTransferWithSides(playerIn, index);
        }

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack ret = slot.getItem().copy();
        ItemStack stack = slot.getItem().copy();
        boolean nothingDone;

        if (index == 0) { // Crafting output
            craftMatrix.setDoNotCallUpdates(true);
            try {
                nothingDone = !moveToPlayerInventory(stack);
            } finally {
                craftMatrix.setDoNotCallUpdates(false);
                craftMatrix.setChanged();
            }
        } else if (index < 10) { // Crafting grid
            nothingDone = !moveToPlayerInventory(stack);
        } else { // Player inventory
            nothingDone = !moveToCraftingStation(stack);
        }

        if (nothingDone) {
            return ItemStack.EMPTY;
        }
        return notifySlotAfterTransfer(playerIn, stack, ret, slot);
    }

    protected ItemStack handleTransferWithSides(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack ret = slot.getItem().copy();
        ItemStack stack = ret.copy();
        boolean nothingDone;

        if (index == 0) { // Output slot
            craftMatrix.setDoNotCallUpdates(true);
            try {
                nothingDone = !refillSideInventory(stack);
                nothingDone &= !moveToPlayerInventory(stack);
                nothingDone &= !mergeItemStackMove(stack, 10, 10 + getVisibleSideSlotCount());
            } finally {
                craftMatrix.setDoNotCallUpdates(false);
                craftMatrix.setChanged();
            }
        } else if (index < 10) { // Crafting grid
            nothingDone = !refillSideInventory(stack);
            nothingDone &= !moveToPlayerInventory(stack);
            nothingDone &= !moveToSideInventory(stack);
        } else if (index < 10 + getVisibleSideSlotCount()) { // Visible side container
            nothingDone = !moveToCraftingStation(stack);
            nothingDone &= !moveToPlayerInventory(stack);
        } else if (index < 10 + sideSlots.size()) { // Hidden side slot, ignore
            return ItemStack.EMPTY;
        } else if (index >= 10 + sideSlots.size()) { // Player inventory
            nothingDone = !moveToCraftingStation(stack);
            nothingDone &= !moveToSideInventory(stack);
        } else {
            return ItemStack.EMPTY;
        }

        if (nothingDone) {
            return ItemStack.EMPTY;
        }
        return notifySlotAfterTransfer(player, stack, ret, slot);
    }



    protected static void slotChangedCraftingGrid(
            AbstractContainerMenu pMenu,
            Level pLevel,
            Player pPlayer,
            CraftingContainer pCraftSlots,
            ResultContainer pResultSlots,
            RecipeHolder<CraftingRecipe> pRecipe
    ) {
        if (!pLevel.isClientSide) {
            CraftingInput craftinginput = pCraftSlots.asCraftInput();
            ServerPlayer serverplayer = (ServerPlayer) pPlayer;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> optional = pLevel.getServer()
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftinginput, pLevel, pRecipe);
            if (optional.isPresent()) {
                RecipeHolder<CraftingRecipe> recipeholder = optional.get();
                CraftingRecipe craftingrecipe = recipeholder.value();
                if (pResultSlots.setRecipeUsed(pLevel, serverplayer, recipeholder)) {
                    ItemStack itemstack1 = craftingrecipe.assemble(craftinginput, pLevel.registryAccess());
                    if (itemstack1.isItemEnabled(pLevel.enabledFeatures())) {
                        itemstack = itemstack1;
                    }
                }
            }

            pResultSlots.setItem(0, itemstack);
            pMenu.setRemoteSlot(0, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(pMenu.containerId, pMenu.incrementStateId(), 0, itemstack));
        }
    }

    public boolean sameGui(CraftingStationMenu otherContainer) {
        return this.tileEntity == otherContainer.tileEntity;
    }

    protected ItemStack notifySlotAfterTransfer(Player player, ItemStack stack, ItemStack original, Slot slot) {
        // notify slot
        slot.onQuickCraft(stack, original);

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        // update slot we pulled from
        slot.set(stack);
        slot.onTake(player, stack);

        if (slot.hasItem() && slot.getItem().isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }

        return original;
    }

    //return true if anything happened
    protected boolean moveToSideInventory(ItemStack stack) {
        if (!hasSideContainers()) return false;

        SideContainerWrapper wrapper = getCurrentHandler();
        if (wrapper == null) return false;

        boolean moved = false;
        ItemStack remaining = stack.copy();

        // First try to merge with existing stacks
        for (int i = 0; i < wrapper.$getSlotCount(); i++) {
            if (!wrapper.$valid(i)) continue;

            ItemStack inSlot = wrapper.$getStack(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(remaining, inSlot)) {
                ItemStack result = wrapper.$insert(i, remaining, false);
                if (result.getCount() != remaining.getCount()) {
                    remaining = result;
                    moved = true;
                    if (remaining.isEmpty()) break;
                }
            }
        }

        // Then try empty slots
        if (!remaining.isEmpty()) {
            for (int i = 0; i < wrapper.$getSlotCount(); i++) {
                if (!wrapper.$valid(i)) continue;

                if (wrapper.$getStack(i).isEmpty()) {
                    ItemStack result = wrapper.$insert(i, remaining, false);
                    if (result.getCount() != remaining.getCount()) {
                        remaining = result;
                        moved = true;
                        if (remaining.isEmpty()) break;
                    }
                }
            }
        }

        if (moved) {
            stack.setCount(remaining.getCount());
        }

        return moved;
    }

    protected boolean moveToPlayerInventory(ItemStack stack) {
        int start = 10 + (hasSideContainers() ? sideSlots.size() : 0);
        return moveItemStackTo(stack, start, this.slots.size(), true);
    }

    protected boolean refillSideInventory(ItemStack itemStack) {
        return this.mergeItemStackRefillSideContainer(itemStack, 0, subContainerSize());
    }

    protected boolean moveToCraftingStation(ItemStack itemstack) {
        return this.moveItemStackTo(itemstack, 1, 10, false);
    }

    // Fix for a vanilla bug: doesn't take Slot.getMaxStackSize into account
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
        boolean didSomething = mergeItemStackRefill(stack, startIndex, endIndex);
        if (!stack.isEmpty()) didSomething |= mergeItemStackMove(stack, startIndex, endIndex);
        return didSomething;
    }

    // only refills items that are already present
    //return true if successful
    protected boolean mergeItemStackRefill(ItemStack stack, int startIndex, int endIndex) {
        if (stack.isEmpty()) return false;

        boolean didSomething = false;

        Slot targetSlot;
        ItemStack slotStack;

        if (stack.isStackable()) {

            for (int k = startIndex; k < endIndex; k++) {
                if (stack.isEmpty()) break;
                targetSlot = this.slots.get(k);
                slotStack = targetSlot.getItem();

                if (!slotStack.isEmpty()
                        && ItemStack.isSameItemSameComponents(stack, slotStack)
                        && this.canTakeItemForPickAll(stack, targetSlot)) {
                    int l = slotStack.getCount() + stack.getCount();
                    int limit = targetSlot.getMaxStackSize(stack);

                    if (l <= limit) {
                        stack.setCount(0);
                        slotStack.setCount(l);
                        targetSlot.setChanged();
                        didSomething = true;
                    } else if (slotStack.getCount() < limit) {
                        stack.shrink(limit - slotStack.getCount());
                        slotStack.setCount(limit);
                        targetSlot.setChanged();
                        didSomething = true;
                    }
                }
            }
        }
        return didSomething;
    }

    protected boolean mergeItemStackMove(ItemStack stack, int startIndex, int endIndex) {
        if (stack.isEmpty()) return false;

        // Ensure the indices are within bounds
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(this.slots.size(), endIndex);

        boolean didSomething = false;

        for (int k = startIndex; k < endIndex; k++) {
            Slot targetSlot = this.slots.get(k);
            ItemStack slotStack = targetSlot.getItem();

            if (slotStack.isEmpty() && targetSlot.mayPlace(stack) && this.canTakeItemForPickAll(stack, targetSlot)) {
                int limit = targetSlot.getMaxStackSize(stack);
                ItemStack stack2 = stack.copy();
                if (stack2.getCount() > limit) {
                    stack2.setCount(limit);
                    stack.shrink(limit);
                } else {
                    stack.setCount(0);
                }
                targetSlot.set(stack2);
                targetSlot.setChanged();
                didSomething = true;

                if (stack.isEmpty()) {
                    break;
                }
            }
        }
        return didSomething;
    }

    // only moves items into empty slots
    protected boolean mergeItemStackMoveSideContainer(ItemStack stack, int startIndex, int endIndex) {
        if (stack.isEmpty()) return false;

        boolean didSomething = false;
        SideContainerWrapper sideContainerWrapper = getCurrentHandler();
        ItemStack remainder = stack.copy();
        for (int k = startIndex; k < endIndex; k++) {
            ItemStack slotStack = sideContainerWrapper.$getStack(k);
            if (slotStack.isEmpty() && sideContainerWrapper.$valid(k)){ // Forge: Make sure to respect isItemValid in the slot.
                remainder = sideContainerWrapper.$insert(k,remainder,false);
                didSomething = remainder != stack;

                if (remainder.isEmpty()) {
                    break;
                }
            }
        }

        if (didSomething) {
            stack.setCount(remainder.getCount());
        }

        return didSomething;
    }

    // only refills items that are already present
    //return true if successful
    protected boolean mergeItemStackRefillSideContainer(ItemStack stack, int startIndex, int endIndex) {
        if (stack.isEmpty()) return false;

        SideContainerWrapper sideContainerWrapper = getCurrentHandler();

        boolean didSomething = false;

        ItemStack slotStack;

        if (stack.isStackable()) {
            ItemStack remainder = stack.copy();

            for (int k = startIndex; k < endIndex; k++) {
                if (stack.isEmpty()) break;
                slotStack = sideContainerWrapper.$getStack(k);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, slotStack)) {
                    remainder = sideContainerWrapper.$insert(k,remainder,false);
                    didSomething = remainder != stack;

                    if (remainder.isEmpty()) {
                        break;
                    }
                }
            }
            if (didSomething) {
                stack.setCount(remainder.getCount());
            }
        }
        return didSomething;
    }


    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }

    public boolean needsScroll() {
        SideContainerWrapper handler = getCurrentHandler();
        if (handler == null) {
            return false;
        }
        return handler.$getSlotCount() > VISIBLE_SLOTS;
    }

    protected Direction currentContainer;

    public void setCurrentContainer(Direction currentContainer) {
        this.currentContainer = currentContainer;
        this.firstSlot = 0;
        refreshSideSlots();
    }


    public enum ButtonAction {
        CLEAR, TAB_0, TAB_1, TAB_2, TAB_3, TAB_4, TAB_5;
        static final ButtonAction[] VALUES = values();
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int id) {
        if (id < 0 || id >= ButtonAction.VALUES.length) return false;
        ButtonAction buttonAction = ButtonAction.VALUES[id];
        if (pPlayer instanceof ServerPlayer) {
            switch (buttonAction) {
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
    public void removed(Player $$0) {
        super.removed($$0);
        if (!$$0.level().isClientSide) {
            tileEntity.setCurrentContainer(currentContainer);
        }
    }

    public void setFirstSlot(int firstSlot) {
        SideContainerWrapper handler = getCurrentHandler();
        if (handler == null) {
            this.firstSlot = 0;
            return;
        }
        int maxOffset = Math.max(0, handler.$getSlotCount() - VISIBLE_SLOTS);
        this.firstSlot = Mth.clamp(firstSlot, 0, maxOffset);
    }

    public int getFirstSlot() {
        return firstSlot;
    }

    public int getVisibleSideSlotCount() {
        return visibleSideSlotCount;
    }

    private void refreshSideSlots() {
        SideContainerWrapper handler = getCurrentHandler();
        int totalSlots = handler != null ? handler.$getSlotCount() : 0;
        visibleSideSlotCount = Math.min(totalSlots, VISIBLE_SLOTS);

        if (sideSlots.isEmpty()) {
            return;
        }

        boolean scrolling = totalSlots > VISIBLE_SLOTS;
        int xOffset = (scrolling ? -125 : -117);

        for (int i = 0; i < sideSlots.size(); i++) {
            int xPos = HIDDEN_SLOT_POS;
            int yPos = HIDDEN_SLOT_POS;
            if (i < visibleSideSlotCount && totalSlots > 0) {
                int row = i / SLOTS_PER_ROW;
                int col = i % SLOTS_PER_ROW;
                xPos = xOffset + col * 18;
                yPos = 17 + row * 18;
            }

            SideContainerSlot existing = sideSlots.get(i);
            if (existing.x != xPos || existing.y != yPos) {
                SideContainerSlot replacement = new SideContainerSlot(i, xPos, yPos, this);
                sideSlots.set(i, replacement);
                int slotListIndex = sideContainerStartIndex + i;
                if (slotListIndex < this.slots.size()) {
                    this.slots.set(slotListIndex, replacement);
                }
            }
        }

        int maxOffset = Math.max(0, totalSlots - VISIBLE_SLOTS);
        this.firstSlot = Mth.clamp(this.firstSlot, 0, maxOffset);
    }

    private boolean isValidSideSlot(SideContainerWrapper handler, int slot) {
        return handler != null && slot >= 0 && slot < handler.$getSlotCount();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (hasSideContainers()) {
            syncSideContainers();
        }
    }


    public void syncSideContainers() {
        for (Map.Entry<Direction, BlockEntity> entry : blockEntityMap.entrySet()) {
            Direction direction = entry.getKey();
            BlockEntity blockEntity = entry.getValue();
            SideContainerWrapper wrapper = Services.PLATFORM.getWrapper(blockEntity);
            if (wrapper != null) {
                for (int i = 0; i < wrapper.$getSlotCount(); i++) {
                    Services.PLATFORM.sendToClient(new S2CSideSetSideContainerSlot(wrapper.$getStack(i), direction, i), (ServerPlayer) player);
                }
            }
        }
    }


    public void synchronizeSlotToRemote(int pSlotIndex, ItemStack pStack, Supplier<ItemStack> pSupplier) {
        if (!this.suppressRemoteUpdates) {
            ItemStack itemstack = this.remoteSlots.get(pSlotIndex);
            if (true) {
                ItemStack itemstack1 = pSupplier.get();
                this.remoteSlots.set(pSlotIndex, itemstack1);
                if (this.synchronizer != null) {
                    // Forge: Only synchronize a slot change if the itemstack actually changed in a way that is relevant to the client (i.e. share tag changed)
                    this.synchronizer.sendSlotChange(this, pSlotIndex, itemstack1);
                }
            }
        }
    }

    // Getter for side container start index, considering direction
    public int getSideContainerStartIndex(Direction direction) {
        if (blockEntityMap.containsKey(direction)) {
            // Compute the start index based on the direction
            // This is just an example; adjust the index logic as necessary
            return this.sideContainerStartIndex;
        } else {
            return 0;  // Return a default value if no side container in the given direction
        }
    }

    // Setter for side container start index
    public void setSideContainerStartIndex(int startIndex) {
        this.sideContainerStartIndex = startIndex;
    }

    // Getter for player inventory start index
    public int getPlayerInventoryStartIndex() {
        return this.playerInventoryStartIndex;
    }

    // Setter for player inventory start index
    public void setPlayerInventoryStartIndex(int startIndex) {
        this.playerInventoryStartIndex = startIndex;
    }

}
