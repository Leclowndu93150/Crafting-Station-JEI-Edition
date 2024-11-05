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

        int craftingGridSize = 9;
        for (int i = 1; i <= craftingGridSize; i++) {
            Slot slot = container.getSlot(i);
            slots.add(slot);
        }

        return slots;
    }

    @Override
    public @NotNull List<Slot> getInventorySlots(@NotNull CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        // Dynamically add slots from all connected side containers
        for (Map.Entry<Direction, BlockEntity> entry : container.blockEntityMap.entrySet()) {
            SideContainerWrapper sideContainerWrapper = Services.PLATFORM.getWrapper(entry.getValue());
            int sideSlotCount = sideContainerWrapper.$getSlotCount();

            for (int i = 0; i < sideSlotCount; i++) {
                int adjustedSlotIndex = i + container.getSideContainerStartIndex(entry.getKey());

                if (adjustedSlotIndex < container.slots.size()) { // Check to prevent out-of-bounds
                    slots.add(container.getSlot(adjustedSlotIndex));
                }
            }
        }

        // Add player inventory slots after side containers
        int playerInventoryStart = container.getPlayerInventoryStartIndex();
        for (int i = playerInventoryStart; i < container.slots.size(); i++) {
            Slot slot = container.getSlot(i);
            assert mc.player != null;
            if (slot.allowModification(mc.player)) {
                slots.add(slot);
            }
        }

        return slots;
    }
}
