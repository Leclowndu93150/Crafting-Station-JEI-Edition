package com.leclowndu93150.craftingstationjei.jei;

import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftingStationTransferHandler implements IRecipeTransferHandler<CraftingStationMenu, CraftingRecipe> {

    private final IRecipeTransferHandler<CraftingStationMenu, CraftingRecipe> internalHandler;

    public CraftingStationTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.internalHandler = helper.createUnregisteredRecipeTransferHandler(new TransferInfo());
    }

    @Override
    public Class<? extends CraftingStationMenu> getContainerClass() {
        return CraftingStationMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingStationMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.CRAFTING_STATION.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeType.create("minecraft", "crafting", CraftingRecipe.class);
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(CraftingStationMenu container, CraftingRecipe recipe,
                                                IRecipeSlotsView recipeSlots, Player player,
                                                boolean maxTransfer, boolean doTransfer) {
        return internalHandler.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
    }

    private static class TransferInfo implements IRecipeTransferInfo<CraftingStationMenu, CraftingRecipe> {

        @Override
        public Class<? extends CraftingStationMenu> getContainerClass() {
            return CraftingStationMenu.class;
        }

        @Override
        public Optional<MenuType<CraftingStationMenu>> getMenuType() {
            return Optional.of(ModMenuTypes.CRAFTING_STATION.get());
        }

        @Override
        public RecipeType<CraftingRecipe> getRecipeType() {
            return RecipeType.create("minecraft", "crafting", CraftingRecipe.class);
        }

        @Override
        public boolean canHandle(CraftingStationMenu container, CraftingRecipe recipe) {
            return true;
        }

        @Override
        public List<Slot> getRecipeSlots(CraftingStationMenu container, CraftingRecipe recipe) {
            List<Slot> slots = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                slots.add(container.getSlot(i));
            }
            return slots;
        }

        @Override
        public List<Slot> getInventorySlots(CraftingStationMenu container, CraftingRecipe recipe) {
            List<Slot> slots = new ArrayList<>();

            for (CraftingStationMenu.SideContainerSlot sideSlot : container.getSideSlots()) {
                slots.add(sideSlot);
            }

            for (int i = container.getPlayerInventoryStartIndex(); i < container.slots.size(); i++) {
                slots.add(container.getSlot(i));
            }

            return slots;
        }
    }
}
