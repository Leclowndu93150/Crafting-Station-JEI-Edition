package com.leclowndu93150.craftingstationjei.jei;

import com.leclowndu93150.craftingstationjei.client.CraftingStationScreen;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

public class CraftingStationGuiContainerHandler implements IGuiContainerHandler<CraftingStationScreen> {

    @Override
    public List<Rect2i> getGuiExtraAreas(CraftingStationScreen screen) {
        List<Rect2i> areas = new ArrayList<>();
        if (screen.getMenu().hasSideContainers()) {
            int guiLeft = screen.getGuiLeft();
            int guiTop = screen.getGuiTop();
            areas.add(new Rect2i(guiLeft - 133, guiTop - 22, 133, screen.getYSize() + 39));
        }
        return areas;
    }
}
