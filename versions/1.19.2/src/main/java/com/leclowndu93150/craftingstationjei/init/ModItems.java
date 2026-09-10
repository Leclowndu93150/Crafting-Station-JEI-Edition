package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Craftingstationjei.MODID);

    public static final RegistryObject<Item> CRAFTING_STATION = ITEMS.register("crafting_station",
            () -> new BlockItem(ModBlocks.CRAFTING_STATION.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));

    public static final RegistryObject<Item> CRAFTING_STATION_SLAB = ITEMS.register("crafting_station_slab",
            () -> new BlockItem(ModBlocks.CRAFTING_STATION_SLAB.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
}
