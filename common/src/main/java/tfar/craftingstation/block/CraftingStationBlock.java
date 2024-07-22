package tfar.craftingstation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tfar.craftingstation.blockentity.CraftingStationBlockEntity;
import tfar.craftingstation.platform.Services;


public class CraftingStationBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {

  public static final VoxelShape shape;

  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;


  public CraftingStationBlock(Properties p_i48440_1_) {
    super(p_i48440_1_);
    this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false);
  }

  static {
    VoxelShape[] shapes = new VoxelShape[5];

    shapes[0] = Block.box(0, 12, 0, 16, 16, 16);
    shapes[1] = Block.box(0, 0, 0, 4, 12, 4);
    shapes[2] = Block.box(12, 0, 0, 16, 12, 4);
    shapes[3] = Block.box(0, 0, 12, 4, 12, 16);
    shapes[4] = Block.box(12, 0, 12, 16, 12, 16);

    shape = Shapes.or(shapes[0], shapes[1], shapes[2], shapes[3], shapes[4]);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState pState, Level level, BlockPos pos, Player player, BlockHitResult pHitResult) {
    if (!level.isClientSide) {
      BlockEntity tileEntity = level.getBlockEntity(pos);
      if (tileEntity instanceof MenuProvider) {
        Services.PLATFORM.openMenu((ServerPlayer)player, (MenuProvider) tileEntity,pos);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
    BlockEntity te = world.getBlockEntity(pos);
    return te instanceof CraftingStationBlockEntity ? (MenuProvider) te : null;
  }

  @Override
  public void onRemove(BlockState state,Level worldIn,BlockPos pos, BlockState newState, boolean isMoving) {
    if (state.getBlock() != newState.getBlock()) {
      BlockEntity tileentity = worldIn.getBlockEntity(pos);
      if (tileentity instanceof CraftingStationBlockEntity craftingStationBlock) {
        dropItems(craftingStationBlock.input, worldIn, pos);
        worldIn.updateNeighbourForOutputSignal(pos, this);
      }
      super.onRemove(state, worldIn, pos, newState, isMoving);
    }
  }

  @Override
  public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
    super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
  }


  public static void dropItems(SimpleContainer inv, Level world, BlockPos pos) {
    Containers.dropContents(world,pos,inv);
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
    return shape;
  }

  public BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  public BlockState mirror(BlockState state, Mirror mirror) {
    return state.rotate(mirror.getRotation(state.getValue(FACING)));
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_206840_1_) {
    p_206840_1_.add(WATERLOGGED,FACING);
  }

  public FluidState getFluidState(BlockState p_204507_1_) {
    return p_204507_1_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_204507_1_);
  }

  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    LevelAccessor level = ctx.getLevel();
    BlockPos pos = ctx.getClickedPos();
    boolean water = level.getFluidState(pos).getType() == Fluids.WATER;
    return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(WATERLOGGED, water);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return Services.PLATFORM.create(pPos,pState);
  }
}
