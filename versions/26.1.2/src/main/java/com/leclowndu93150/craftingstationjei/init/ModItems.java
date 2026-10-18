package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Craftingstationjei.MODID)
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Craftingstationjei.MODID);

    public static final DeferredItem<BlockItem> CRAFTING_STATION = ITEMS.registerItem("crafting_station",
            props -> new BlockItem(ModBlocks.CRAFTING_STATION.get(), props.useBlockDescriptionPrefix()));

    public static final DeferredItem<BlockItem> CRAFTING_STATION_SLAB = ITEMS.registerItem("crafting_station_slab",
            props -> new BlockItem(ModBlocks.CRAFTING_STATION_SLAB.get(), props.useBlockDescriptionPrefix()));

    @SubscribeEvent
    public static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CRAFTING_STATION.get());
            event.accept(CRAFTING_STATION_SLAB.get());
        }
    }
}
