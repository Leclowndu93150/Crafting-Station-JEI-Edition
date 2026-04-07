package com.leclowndu93150.craftingstationjei.compat;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.blay09.mods.craftingtweaks.api.CraftingGridBuilder;
import net.blay09.mods.craftingtweaks.api.CraftingGridProvider;
import net.blay09.mods.craftingtweaks.api.CraftingTweaksAPI;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CraftingTweaksCompat {

    public static void init() {
        CraftingTweaksAPI.registerCraftingGridProvider(new CraftingGridProvider() {
            @Override
            public String getModId() {
                return Craftingstationjei.MODID;
            }

            @Override
            public boolean handles(AbstractContainerMenu menu) {
                return menu instanceof CraftingStationMenu;
            }

            @Override
            public void buildCraftingGrids(CraftingGridBuilder builder, AbstractContainerMenu menu) {
                builder.addGrid(1, 3, 3);
            }
        });
    }
}
