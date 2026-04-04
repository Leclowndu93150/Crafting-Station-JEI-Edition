package com.leclowndu93150.craftingstationjei.jei;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.client.CraftingStationScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = new ResourceLocation(Craftingstationjei.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new CraftingStationTransferHandler(registration.getTransferHelper()),
                RecipeType.create("minecraft", "crafting", CraftingRecipe.class)
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CraftingStationScreen.class, new CraftingStationGuiContainerHandler());
    }
}
