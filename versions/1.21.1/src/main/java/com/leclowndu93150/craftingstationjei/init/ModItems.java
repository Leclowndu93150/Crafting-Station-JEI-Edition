package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@EventBusSubscriber(modid = Craftingstationjei.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Craftingstationjei.MODID);

    public static final Supplier<BlockItem> CRAFTING_STATION = ITEMS.register("crafting_station",
            () -> new BlockItem(ModBlocks.CRAFTING_STATION.get(), new Item.Properties()));

    public static final Supplier<BlockItem> CRAFTING_STATION_SLAB = ITEMS.register("crafting_station_slab",
            () -> new BlockItem(ModBlocks.CRAFTING_STATION_SLAB.get(), new Item.Properties()));

    @SubscribeEvent
    public static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CRAFTING_STATION.get());
            event.accept(CRAFTING_STATION_SLAB.get());
        }
    }
}
