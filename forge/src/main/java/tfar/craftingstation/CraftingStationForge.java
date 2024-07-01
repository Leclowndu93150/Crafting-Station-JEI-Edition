package tfar.craftingstation;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tfar.craftingstation.client.ModClientForge;
import tfar.craftingstation.datagen.ModDatagen;
import tfar.craftingstation.init.ModBlockEntityTypes;
import tfar.craftingstation.init.ModBlocks;
import tfar.craftingstation.init.ModMenuTypes;
import tfar.craftingstation.menu.CraftingStationMenu;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CraftingStation.MOD_ID)
public class CraftingStationForge {
    // Directly reference a log4j logger.



    public static final Logger LOGGER = LogManager.getLogger();

    public CraftingStationForge() {
        // Register the setup method for modloading
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        IEventBus iEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        iEventBus.addListener(this::setup);
        iEventBus.addListener(this::enqueueIMC);
        iEventBus.addListener(ModDatagen::gather);
        iEventBus.addListener(RegistryEvents::block);
        iEventBus.addListener(this::addCreative);
        if (FMLEnvironment.dist.isClient()) {
            ModClientForge.setup(iEventBus);
        }
        CraftingStation.init();
    }

    public static final Configs.Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Configs.Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<Configs.Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Configs.Client::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
        final Pair<Configs.Server, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(Configs.Server::new);
        SERVER_SPEC = specPair2.getRight();
        SERVER = specPair2.getLeft();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.crafting_station);
            event.accept(ModBlocks.crafting_station_slab);
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo("craftingtweaks", "RegisterProvider", () -> {
            CompoundTag tagCompound = new CompoundTag();
            tagCompound.putString("ContainerClass", CraftingStationMenu.class.getName());
            tagCompound.putString("AlignToGrid", "left");
            return tagCompound;
        });
    }

    public static class RegistryEvents {
        @SubscribeEvent
        public static void block(final RegisterEvent event) {
            // register a new block here
            event.register(Registries.BLOCK, CraftingStation.id("crafting_station"), () -> ModBlocks.crafting_station);
            event.register(Registries.BLOCK, CraftingStation.id("crafting_station_slab"), () -> ModBlocks.crafting_station_slab);
            // register a new item here
            Item.Properties properties = new Item.Properties();
            event.register(Registries.ITEM, CraftingStation.id("crafting_station"), () -> new BlockItem(ModBlocks.crafting_station, properties));
            event.register(Registries.ITEM, CraftingStation.id("crafting_station_slab"), () -> new BlockItem(ModBlocks.crafting_station_slab, properties));
            event.register(Registries.MENU, CraftingStation.id("crafting_station"), () -> ModMenuTypes.crafting_station);
            event.register(Registries.BLOCK_ENTITY_TYPE, CraftingStation.id("crafting_station"), () -> ModBlockEntityTypes.crafting_station);
        }

    }
}
