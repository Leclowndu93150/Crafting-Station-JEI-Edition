package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Craftingstationjei.MODID);

    public static final Supplier<MenuType<CraftingStationMenu>> CRAFTING_STATION =
            MENU_TYPES.register("crafting_station",
                    () -> IMenuTypeExtension.create(CraftingStationMenu::new));
}
