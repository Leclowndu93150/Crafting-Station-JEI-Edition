package tfar.craftingstation.jei;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;
import tfar.craftingstation.client.CraftingStationScreen;
import java.util.ArrayList;
import java.util.List;

public class CraftingStationGuiContainerHandler implements IGuiContainerHandler<CraftingStationScreen> {

    @Override
    public List<Rect2i> getGuiExtraAreas(CraftingStationScreen guiContainer) {
        ArrayList<Rect2i> list = new ArrayList<>();
        if (guiContainer.getMenu().hasSideContainers()) {
            int width = 130;
            int left = guiContainer.getGuiLeft() - width;
            int top = guiContainer.getGuiTop() - 22;
            int height = guiContainer.getYSize() + 39;

            Rect2i sideMenuSection = new Rect2i(left, top, width, height);
            list.add(sideMenuSection);
        }

        return list;
    }
}
