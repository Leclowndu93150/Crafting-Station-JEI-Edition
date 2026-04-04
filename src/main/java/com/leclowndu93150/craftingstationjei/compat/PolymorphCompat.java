package com.leclowndu93150.craftingstationjei.compat;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.common.capability.IPlayerRecipeData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Optional;

public class PolymorphCompat {

    public static Optional<CraftingRecipe> getRecipe(AbstractContainerMenu menu, CraftingContainer craftMatrix,
                                                     Level level, Player player) {
        Optional<? extends IPlayerRecipeData> maybeData = PolymorphApi.common().getRecipeData(player);
        maybeData.ifPresent(data -> data.setContainerMenu(menu));
        return maybeData
                .map(data -> data.getRecipe(RecipeType.CRAFTING, craftMatrix, level, new ArrayList<>()))
                .orElseGet(() -> level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftMatrix, level));
    }
}
