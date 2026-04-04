package com.leclowndu93150.craftingstationjei.jei;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftingStationTransferHandler implements IRecipeTransferHandler<CraftingStationMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<CraftingStationMenu, RecipeHolder<CraftingRecipe>> internalHandler;

    public CraftingStationTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
        this.internalHandler = helper.createUnregisteredRecipeTransferHandler(new TransferInfo());
    }

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
        return RecipeType.createFromVanilla(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe,
                                               IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer,
                                               boolean doTransfer) {
        return internalHandler.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
    }

    private static class TransferInfo implements IRecipeTransferInfo<CraftingStationMenu, RecipeHolder<CraftingRecipe>> {

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
            return RecipeType.createFromVanilla(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        }

        @Override
        public boolean canHandle(CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
            return true;
        }

        @Override
        public List<Slot> getRecipeSlots(CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
            List<Slot> slots = new ArrayList<>();
            for (int i = 1; i < 10; i++) {
                slots.add(container.getSlot(i));
            }
            return slots;
        }

        @Override
        public List<Slot> getInventorySlots(CraftingStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
            List<Slot> slots = new ArrayList<>();
            for (int i = container.getSideContainerStartIndex(); i < container.getPlayerInventoryStartIndex(); i++) {
                slots.add(container.getSlot(i));
            }
            for (int i = container.getPlayerInventoryStartIndex(); i < container.slots.size(); i++) {
                slots.add(container.getSlot(i));
            }
            return slots;
        }
    }
}
