package com.leclowndu93150.craftingstationjei.block;

import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CraftingStationBlock extends BaseEntityBlock {

    public static final MapCodec<CraftingStationBlock> CODEC = simpleCodec(CraftingStationBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape TOP = Block.box(0, 12, 0, 16, 16, 16);
    private static final VoxelShape LEG_NW = Block.box(0, 0, 0, 4, 12, 4);
    private static final VoxelShape LEG_NE = Block.box(12, 0, 0, 16, 12, 4);
    private static final VoxelShape LEG_SW = Block.box(0, 0, 12, 4, 12, 16);
    private static final VoxelShape LEG_SE = Block.box(12, 0, 12, 16, 12, 16);
    private static final VoxelShape SHAPE = Shapes.or(TOP, LEG_NW, LEG_NE, LEG_SW, LEG_SE);

    public CraftingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CraftingStationBlockEntity csbe) {
                serverPlayer.openMenu(csbe, buf -> {
                    buf.writeBlockPos(pos);
                    CraftingStationMenu.writeSideSlotCounts(buf, level, pos);
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CraftingStationBlockEntity csbe) {
                Containers.dropContents(level, pos, csbe.getInput());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
