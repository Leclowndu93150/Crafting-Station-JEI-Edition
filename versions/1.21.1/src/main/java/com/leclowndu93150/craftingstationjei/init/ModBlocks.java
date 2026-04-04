package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.block.CraftingStationBlock;
import com.leclowndu93150.craftingstationjei.block.CraftingStationSlabBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Craftingstationjei.MODID);

    public static final Supplier<CraftingStationBlock> CRAFTING_STATION = BLOCKS.register("crafting_station",
            () -> new CraftingStationBlock(Block.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));

    public static final Supplier<CraftingStationSlabBlock> CRAFTING_STATION_SLAB = BLOCKS.register("crafting_station_slab",
            () -> new CraftingStationSlabBlock(Block.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
}
