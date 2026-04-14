package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.block.CraftingStationBlock;
import com.leclowndu93150.craftingstationjei.block.CraftingStationSlabBlock;
import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CraftingStationBlockEntityRenderer implements BlockEntityRenderer<CraftingStationBlockEntity, CraftingStationBlockEntityRenderer.RenderState> {

    private final ItemModelResolver itemModelResolver;

    public CraftingStationBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(CraftingStationBlockEntity blockEntity, RenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        int seed = (int) blockEntity.getBlockPos().asLong();
        state.items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            ItemStack stack = blockEntity.getInput().getItem(i);
            if (!stack.isEmpty()) {
                this.itemModelResolver.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + i);
            }
            state.items.add(itemState);
        }

        BlockState blockState = blockEntity.getBlockState();
        state.topY = 1.0;
        if (blockState.getBlock() instanceof SlabBlock) {
            SlabType type = blockState.getValue(SlabBlock.TYPE);
            if (type == SlabType.BOTTOM) {
                state.topY = 0.5;
            }
        }

        Direction facing = Direction.NORTH;
        if (blockState.hasProperty(CraftingStationBlock.FACING)) {
            facing = blockState.getValue(CraftingStationBlock.FACING);
        } else if (blockState.hasProperty(CraftingStationSlabBlock.FACING)) {
            facing = blockState.getValue(CraftingStationSlabBlock.FACING);
        }
        state.yRot = -facing.toYRot();

        if (blockEntity.getLevel() != null) {
            state.lightCoords = LevelRenderer.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().above());
        }
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        for (int i = 0; i < state.items.size(); i++) {
            ItemStackRenderState itemState = state.items.get(i);
            if (itemState.isEmpty()) continue;

            boolean blockItem = itemState.usesBlockLight();

            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
            poseStack.translate((i % 3) * 3.0 / 16.0 + 0.3125 - 0.5,
                    state.topY + (blockItem ? 0.0625 : 0.005),
                    (i / 3) * 3.0 / 16.0 + 0.3125 - 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = blockItem ? 0.25F : 0.175F;
            poseStack.scale(scale, scale, scale);

            itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public static class RenderState extends BlockEntityRenderState {
        public List<ItemStackRenderState> items = Collections.emptyList();
        public double topY = 1.02;
        public float yRot = 0.0F;
    }
}
