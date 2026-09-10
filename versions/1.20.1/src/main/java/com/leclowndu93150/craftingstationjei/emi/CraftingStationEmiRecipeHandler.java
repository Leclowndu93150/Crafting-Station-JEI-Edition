package com.leclowndu93150.craftingstationjei.emi;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CraftingStationEmiRecipeHandler implements StandardRecipeHandler<CraftingStationMenu> {

    @Override
    public List<Slot> getInputSources(CraftingStationMenu menu) {
        List<Slot> slots = new ArrayList<>(menu.getSideSlots());
        for (int i = menu.getPlayerInventoryStartIndex(); i < menu.slots.size(); i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(CraftingStationMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }

    @Override
    public Slot getOutputSlot(CraftingStationMenu menu) {
        return menu.getSlot(0);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingStationMenu> context) {
        List<ItemStack> stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), context.getAmount());
        if (stacks == null) return false;
        Minecraft.getInstance().setScreen(context.getScreen());
        return fillByClicks(recipe, context.getScreen(), stacks, context.getDestination());
    }

    private boolean fillByClicks(EmiRecipe recipe, AbstractContainerScreen<CraftingStationMenu> screen,
                                 List<ItemStack> stacks, EmiCraftContext.Destination destination) {
        CraftingStationMenu menu = screen.getMenu();
        if (!menu.getCarried().isEmpty()) return false;
        Minecraft client = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = client.gameMode;
        Player player = client.player;
        int id = menu.containerId;

        for (Slot slot : getCraftingSlots(menu)) {
            gameMode.handleInventoryMouseClick(id, slot.index, 0, ClickType.QUICK_MOVE, player);
        }

        List<Slot> inputs = getInputSources(menu);
        List<Slot> crafting = getCraftingSlots(recipe, menu);
        outer:
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) continue;
            if (i >= crafting.size()) return false;
            Slot target = crafting.get(i);
            if (target == null) return false;
            int needed = stack.getCount();
            for (Slot input : inputs) {
                if (crafting.contains(input)) continue;
                ItemStack available = input.getItem();
                if (!ItemStack.isSameItemSameTags(available, stack)) continue;
                int picked = Math.min(available.getCount(), available.getMaxStackSize());
                gameMode.handleInventoryMouseClick(id, input.index, 0, ClickType.PICKUP, player);
                if (picked <= needed) {
                    needed -= picked;
                    gameMode.handleInventoryMouseClick(id, target.index, 0, ClickType.PICKUP, player);
                } else {
                    while (needed > 0) {
                        gameMode.handleInventoryMouseClick(id, target.index, 1, ClickType.PICKUP, player);
                        needed--;
                    }
                    gameMode.handleInventoryMouseClick(id, input.index, 0, ClickType.PICKUP, player);
                }
                if (needed == 0) continue outer;
            }
            return false;
        }

        Slot output = getOutputSlot(menu);
        if (destination == EmiCraftContext.Destination.CURSOR) {
            gameMode.handleInventoryMouseClick(id, output.index, 0, ClickType.PICKUP, player);
        } else if (destination == EmiCraftContext.Destination.INVENTORY) {
            gameMode.handleInventoryMouseClick(id, output.index, 0, ClickType.QUICK_MOVE, player);
        }
        return true;
    }
}
