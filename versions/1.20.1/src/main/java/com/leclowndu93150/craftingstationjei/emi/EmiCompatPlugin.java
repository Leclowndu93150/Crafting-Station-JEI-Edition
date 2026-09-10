package com.leclowndu93150.craftingstationjei.emi;

import com.leclowndu93150.craftingstationjei.client.CraftingStationScreen;
import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

@EmiEntrypoint
public class EmiCompatPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(ModMenuTypes.CRAFTING_STATION.get(), new CraftingStationEmiRecipeHandler());
        registry.addExclusionArea(CraftingStationScreen.class, (screen, consumer) -> {
            if (screen.getMenu().hasSideContainers()) {
                consumer.accept(new Bounds(screen.getGuiLeft() - 133, screen.getGuiTop() - 22, 133, screen.getYSize() + 39));
            }
        });
    }
}
