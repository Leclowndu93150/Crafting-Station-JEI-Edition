package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.block.CraftingStationBlock;
import com.leclowndu93150.craftingstationjei.block.CraftingStationSlabBlock;
import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public class CraftingStationBlockEntityRenderer implements BlockEntityRenderer<CraftingStationBlockEntity> {

    private final ItemRenderer itemRenderer;

    public CraftingStationBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(CraftingStationBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        int light = be.getLevel() != null
                ? LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above())
                : 15728880;

        BlockState state = be.getBlockState();
        double topY = 1.0;
        if (state.getBlock() instanceof SlabBlock) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (type == SlabType.BOTTOM) {
                topY = 0.5;
            }
        }

        Direction facing = Direction.NORTH;
        if (state.hasProperty(CraftingStationBlock.FACING)) {
            facing = state.getValue(CraftingStationBlock.FACING);
        } else if (state.hasProperty(CraftingStationSlabBlock.FACING)) {
            facing = state.getValue(CraftingStationSlabBlock.FACING);
        }
        float yRot = -facing.toYRot();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = be.getInput().getItem(i);
            if (stack.isEmpty()) continue;

            BakedModel model = this.itemRenderer.getModel(stack, be.getLevel(), null, 0);
            boolean blockItem = model.isGui3d();

            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.translate((i % 3) * 3.0 / 16.0 + 0.3125 - 0.5,
                    topY + (blockItem ? 0.0625 : 0.005),
                    (i / 3) * 3.0 / 16.0 + 0.3125 - 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = blockItem ? 0.25F : 0.175F;
            poseStack.scale(scale, scale, scale);

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay,
                    poseStack, buffer, be.getLevel(),
                    (int) be.getBlockPos().asLong() + i);
            poseStack.popPose();
        }
    }
}
