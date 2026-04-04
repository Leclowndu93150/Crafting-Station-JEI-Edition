package com.leclowndu93150.craftingstationjei.init;

import com.leclowndu93150.craftingstationjei.Craftingstationjei;
import com.leclowndu93150.craftingstationjei.block.CraftingStationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Craftingstationjei.MODID);

    public static final RegistryObject<Block> CRAFTING_STATION = BLOCKS.register("crafting_station",
            () -> new CraftingStationBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));
}
