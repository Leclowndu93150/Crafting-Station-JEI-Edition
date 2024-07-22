package tfar.craftingstation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import tfar.craftingstation.blockentity.CraftingStationBlockEntity;
import tfar.craftingstation.platform.Services;

public class CraftingStationSlabBlock extends SlabBlock implements EntityBlock {


  public CraftingStationSlabBlock(Properties properties) {
    super(properties);
    this.stateDefinition.any().setValue(CraftingStationBlock.FACING, Direction.NORTH);

  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult pHitResult) {
    if (!level.isClientSide) {
      MenuProvider iNamedContainerProvider = getMenuProvider(state,level,pos);
      if (iNamedContainerProvider != null) {
        Services.PLATFORM.openMenu((ServerPlayer)player, iNamedContainerProvider,pos);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
    BlockEntity te = world.getBlockEntity(pos);
    return te instanceof CraftingStationBlockEntity ? (MenuProvider) te : null;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return Services.PLATFORM.create(pPos,pState);
  }

  @Override
  public void onRemove(BlockState state,Level worldIn,BlockPos pos,BlockState newState, boolean isMoving) {
    if (state.getBlock() != newState.getBlock()) {
      BlockEntity tileentity = worldIn.getBlockEntity(pos);
      if (tileentity instanceof CraftingStationBlockEntity craftingStationBlock) {
        CraftingStationBlock.dropItems(craftingStationBlock.input, worldIn, pos);
        worldIn.updateNeighbourForOutputSignal(pos, this);
      }
      super.onRemove(state, worldIn, pos, newState, isMoving);
    }
  }

  @Override
  public BlockState rotate(BlockState p_185499_1_, Rotation p_185499_2_) {
    return p_185499_1_.setValue(CraftingStationBlock.FACING, p_185499_2_.rotate(p_185499_1_.getValue(CraftingStationBlock.FACING)));
  }

  @Override
  public BlockState mirror(BlockState p_185471_1_, Mirror p_185471_2_) {
    return p_185471_1_.rotate(p_185471_2_.getRotation(p_185471_1_.getValue(CraftingStationBlock.FACING)));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_206840_1_) {
    super.createBlockStateDefinition(p_206840_1_);
    p_206840_1_.add(CraftingStationBlock.FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext p_196258_1_) {
    return super.getStateForPlacement(p_196258_1_).setValue(CraftingStationBlock.FACING, p_196258_1_.getHorizontalDirection());
  }
}
