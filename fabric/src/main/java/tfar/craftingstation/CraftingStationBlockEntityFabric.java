package tfar.craftingstation;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import tfar.craftingstation.blockentity.CraftingStationBlockEntity;

public class CraftingStationBlockEntityFabric extends CraftingStationBlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
    public CraftingStationBlockEntityFabric(BlockPos pPos, BlockState pState) {
        super(pPos, pState);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }
}
