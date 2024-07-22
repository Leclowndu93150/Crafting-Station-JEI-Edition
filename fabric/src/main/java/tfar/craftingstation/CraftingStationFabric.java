package tfar.craftingstation;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import tfar.craftingstation.init.ModBlockEntityTypes;
import tfar.craftingstation.init.ModBlocks;
import tfar.craftingstation.init.ModMenuTypes;
import tfar.craftingstation.network.PacketHandler;

public class CraftingStationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CraftingStation.init();
        // register a new block here
        Registry.register(BuiltInRegistries.BLOCK, CraftingStation.id("crafting_station"), ModBlocks.crafting_station);
        Registry.register(BuiltInRegistries.BLOCK, CraftingStation.id("crafting_station_slab"),ModBlocks.crafting_station_slab);
        // register a new item here
        Item.Properties properties = new Item.Properties();
        Registry.register(BuiltInRegistries.ITEM, CraftingStation.id("crafting_station"),new BlockItem(ModBlocks.crafting_station, properties));
        Registry.register(BuiltInRegistries.ITEM, CraftingStation.id("crafting_station_slab"),new BlockItem(ModBlocks.crafting_station_slab, properties));
        Registry.register(BuiltInRegistries.MENU, CraftingStation.id("crafting_station"),ModMenuTypes.crafting_station);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CraftingStation.id("crafting_station"),ModBlockEntityTypes.crafting_station);
        PacketHandler.registerPackets();
    }
}
