package tfar.craftingstation.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import tfar.craftingstation.init.ModBlocks;
import tfar.craftingstation.platform.Services;

@EmiEntrypoint
public class CraftingStationEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(ModBlocks.crafting_station));
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(ModBlocks.crafting_station_slab));
        registry.addRecipeHandler(Services.PLATFORM.customMenu(), new CraftingStationEmiHandler());
    }
}
