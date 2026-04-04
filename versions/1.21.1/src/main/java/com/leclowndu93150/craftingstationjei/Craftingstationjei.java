package com.leclowndu93150.craftingstationjei;

import com.leclowndu93150.craftingstationjei.client.CraftingStationBlockEntityRenderer;
import com.leclowndu93150.craftingstationjei.client.CraftingStationScreen;
import com.leclowndu93150.craftingstationjei.compat.CraftingTweaksCompat;
import com.leclowndu93150.craftingstationjei.init.ModBlockEntityTypes;
import com.leclowndu93150.craftingstationjei.init.ModBlocks;
import com.leclowndu93150.craftingstationjei.init.ModItems;
import com.leclowndu93150.craftingstationjei.init.ModMenuTypes;
import com.leclowndu93150.craftingstationjei.network.C2SScrollPacket;
import com.leclowndu93150.craftingstationjei.network.S2CSideSetSideContainerSlot;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Craftingstationjei.MODID)
public class Craftingstationjei {

    public static final String MODID = "craftingstation";

    public static final TagKey<BlockEntityType<?>> BLACKLISTED = TagKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "blacklisted")
    );

    public Craftingstationjei(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenuTypes.MENU_TYPES.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerPayloads);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("craftingtweaks")) {
            CraftingTweaksCompat.init();
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(C2SScrollPacket.TYPE, C2SScrollPacket.STREAM_CODEC, C2SScrollPacket::handle);
        registrar.playToClient(S2CSideSetSideContainerSlot.TYPE, S2CSideSetSideContainerSlot.STREAM_CODEC, S2CSideSetSideContainerSlot::handle);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.CRAFTING_STATION.get(), CraftingStationScreen::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntityTypes.CRAFTING_STATION.get(), CraftingStationBlockEntityRenderer::new);
        }
    }
}
