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
import org.jetbrains.annotations.NotNull;
import tfar.craftingstation.menu.CraftingStationMenu;
import tfar.craftingstation.platform.Services;
import tfar.craftingstation.util.SideContainerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CraftingStationTransferHandler implements IRecipeTransferInfo<CraftingStationMenu, RecipeHolder<CraftingRecipe>> {

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
        return true; // Customize condition based on specific needs
    }

    @Override
    public @NotNull List<Slot> getRecipeSlots(@NotNull CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>();
        // Add the 3x3 crafting grid slots (slots 1-9)
        for (int i = 1; i <= 9; i++) {
            Slot slot = container.getSlot(i);
            slots.add(slot);
        }
        return slots;
    }

    @Override
    public @NotNull List<Slot> getInventorySlots(@NotNull CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        // Loop through the block entities in the blockEntityMap to get side container slots
        for (Map.Entry<Direction, BlockEntity> entry : container.blockEntityMap.entrySet()) {
            SideContainerWrapper sideContainerWrapper = Services.PLATFORM.getWrapper(entry.getValue());

            // Loop through each slot in the side container and add it to the list
            for (int i = 0; i < sideContainerWrapper.$getSlotCount(); i++) {
                int adjustedSlotIndex = i + container.getSideContainerStartIndex(entry.getKey());
                slots.add(container.getSlot(adjustedSlotIndex));
            }
        }

        // Add player inventory slots after side containers (typically slots 10+)
        for (int i = container.getPlayerInventoryStartIndex(); i < container.slots.size(); i++) {
            Slot slot = container.getSlot(i);
            assert mc.player != null;
            if (slot.allowModification(mc.player)) {
                slots.add(slot);
            }
        }

        return slots;
    }
}
