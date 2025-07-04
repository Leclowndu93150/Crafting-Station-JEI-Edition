package tfar.craftingstation.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import tfar.craftingstation.menu.CraftingStationMenu;

import java.util.List;
import java.util.stream.IntStream;

public class CraftingStationEmiHandler implements StandardRecipeHandler<CraftingStationMenu> {
    @Override
    public List<Slot> getInputSources(CraftingStationMenu menu) {
        return menu.slots.stream().filter(slot -> slot.index != 0).toList();
    }

    @Override
    public List<Slot> getCraftingSlots(CraftingStationMenu menu) {
        return IntStream.range(1, 1 + 9)
                .mapToObj(menu::getSlot)
                .toList();
    }

    @Override
    public @Nullable Slot getOutputSlot(CraftingStationMenu menu) {
        return menu.getSlot(0);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
    }
}
