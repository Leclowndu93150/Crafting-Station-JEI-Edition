package com.leclowndu93150.craftingstationjei;

import com.leclowndu93150.craftingstationjei.client.CraftingStationBlockEntityRenderer;
import com.leclowndu93150.craftingstationjei.client.CraftingStationScreen;
import com.leclowndu93150.craftingstationjei.init.ModBlockEntityTypes;
import com.leclowndu93150.craftingstationjei.init.ModBlocks;
import com.leclowndu93150.craftingstationjei.init.ModItems;
import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
import com.leclowndu93150.craftingstationjei.network.PacketHandler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(Craftingstationjei.MODID)
public class Craftingstationjei {

    public static final String MODID = "craftingstationjei";

    public static final TagKey<BlockEntityType<?>> BLACKLISTED = TagKey.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(),
            new ResourceLocation(MODID, "blacklisted")
    );

    public Craftingstationjei() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenuTypes.MENU_TYPES.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.init();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                    MenuScreens.register(ModMenuTypes.CRAFTING_STATION.get(), CraftingStationScreen::new)
            );
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntityTypes.CRAFTING_STATION.get(), CraftingStationBlockEntityRenderer::new);
        }
    }
}
