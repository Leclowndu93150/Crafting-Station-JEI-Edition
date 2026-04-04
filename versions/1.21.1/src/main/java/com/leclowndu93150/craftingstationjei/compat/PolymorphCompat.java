package com.leclowndu93150.craftingstationjei.compat;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.common.capability.IPlayerRecipeData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Optional;

public class PolymorphCompat {

    public static Optional<RecipeHolder<CraftingRecipe>> getRecipe(AbstractContainerMenu menu, CraftingContainer craftMatrix,
                                                                    Level level, Player player) {
        Optional<? extends IPlayerRecipeData> maybeData = Optional.ofNullable(PolymorphApi.getInstance().getPlayerRecipeData(player));
        maybeData.ifPresent(data -> data.setContainerMenu(menu));
        return maybeData
                .<Optional<RecipeHolder<CraftingRecipe>>>map(data -> Optional.ofNullable(data.getRecipe(RecipeType.CRAFTING, craftMatrix.asCraftInput(), level, new ArrayList<>())))
                .orElseGet(() -> level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftMatrix.asCraftInput(), level));
    }
}
