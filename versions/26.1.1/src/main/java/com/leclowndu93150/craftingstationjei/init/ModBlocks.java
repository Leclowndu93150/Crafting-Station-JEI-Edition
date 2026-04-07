package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.block.CraftingStationBlock;
import com.leclowndu93150.craftingstationjei.block.CraftingStationSlabBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Craftingstationjei.MODID);

    public static final DeferredBlock<CraftingStationBlock> CRAFTING_STATION = BLOCKS.registerBlock("crafting_station",
            CraftingStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE));

    public static final DeferredBlock<CraftingStationSlabBlock> CRAFTING_STATION_SLAB = BLOCKS.registerBlock("crafting_station_slab",
            CraftingStationSlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE));
}
