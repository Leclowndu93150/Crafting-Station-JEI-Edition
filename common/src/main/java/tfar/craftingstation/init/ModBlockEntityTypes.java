package tfar.craftingstation.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import tfar.craftingstation.blockentity.CraftingStationBlockEntity;
import tfar.craftingstation.platform.Services;

public class ModBlockEntityTypes {
    public static final BlockEntityType<CraftingStationBlockEntity> crafting_station = BlockEntityType.Builder.of(Services.PLATFORM::create, ModBlocks.crafting_station, ModBlocks.crafting_station_slab).build(null);
}
