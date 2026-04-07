package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Craftingstationjei.MODID);

    public static final Supplier<BlockEntityType<CraftingStationBlockEntity>> CRAFTING_STATION =
            BLOCK_ENTITY_TYPES.register("crafting_station",
                    () -> new BlockEntityType<>(CraftingStationBlockEntity::new, ModBlocks.CRAFTING_STATION.get(), ModBlocks.CRAFTING_STATION_SLAB.get()));
}
