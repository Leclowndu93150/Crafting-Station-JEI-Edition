package tfar.craftingstation.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import tfar.craftingstation.menu.CraftingStationMenu;
import tfar.craftingstation.menu.CraftingStationMenu.SideContainerSlot;
import tfar.craftingstation.platform.Services;
import tfar.craftingstation.util.SideContainerWrapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CraftingStationTransferHandler implements IRecipeTransferInfo<CraftingStationMenu, RecipeHolder<CraftingRecipe>> {
    private static final Logger LOGGER = LogManager.getLogger("CraftingStation/TransferHandler");

    @Override
    public Class<? extends CraftingStationMenu> getContainerClass() {
        return CraftingStationMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingStationMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public boolean canHandle(@NotNull CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
        return true;
    }

    @Override
    public @NotNull List<Slot> getRecipeSlots(@NotNull CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            slots.add(container.getSlot(i));
        }
        return slots;
    }

    @Override
    public @NotNull List<Slot> getInventorySlots(@NotNull CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        Map<Direction, SideContainerWrapper> wrapperCache = new EnumMap<>(Direction.class);

        for (Slot slot : container.slots) {
            if (slot instanceof SideContainerSlot sideSlot) {
                Direction direction = sideSlot.getDirection();
                BlockEntity blockEntity = container.blockEntityMap.get(direction);
                if (blockEntity == null) {
                    continue;
                }

                SideContainerWrapper wrapper = wrapperCache.computeIfAbsent(direction, dir -> Services.PLATFORM.getWrapper(blockEntity));
                if (wrapper == null) {
                    continue;
                }

                int actualSlot = sideSlot.getActualSlot();
                int totalSlots = wrapper.$getSlotCount();
                if (actualSlot >= 0 && actualSlot < totalSlots && wrapper.$valid(actualSlot)) {
                    slots.add(slot);
                }
            }
        }

        int playerStart = container.getPlayerInventoryStartIndex();
        if (mc.player != null) {
            for (int i = playerStart; i < container.slots.size(); i++) {
                Slot slot = container.getSlot(i);
                if (slot.container == mc.player.getInventory() && slot.allowModification(mc.player)) {
                    slots.add(slot);
                }
            }
        }

        return slots;
    }
}
