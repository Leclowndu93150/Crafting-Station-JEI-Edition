package com.leclowndu93150.craftingstationjei.jei;

import com.leclowndu93150.craftingstationjei.client.CraftingStationScreen;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;

import java.util.Collection;
import java.util.List;

public class CraftingStationGuiContainerHandler implements IGuiContainerHandler<CraftingStationScreen> {

    @Override
    public List<Rect2i> getGuiExtraAreas(CraftingStationScreen screen) {
        if (screen.getMenu().hasSideContainers()) {
            return List.of(new Rect2i(
                    screen.getGuiLeft() - 133, screen.getGuiTop() - 28,
                    133, screen.getYSize() + 39));
        }
        return List.of();
    }

    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(CraftingStationScreen screen, double mouseX, double mouseY) {
        return List.of(IGuiClickableArea.createBasic(88, 32, 22, 15, RecipeTypes.CRAFTING));
    }
}
