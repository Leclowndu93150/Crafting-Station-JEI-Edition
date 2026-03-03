package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CraftingStationBlockEntityRenderer implements BlockEntityRenderer<CraftingStationBlockEntity> {

    private final ItemRenderer itemRenderer;

    public CraftingStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CraftingStationBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = blockEntity.getInput().getItem(i);
            if (stack.isEmpty()) continue;

            int row = i / 3;
            int col = i % 3;

            poseStack.pushPose();
            poseStack.translate(0.22 + col * 0.28, 1.01, 0.22 + row * 0.28);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.scale(0.22f, 0.22f, 0.22f);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, bufferSource, blockEntity.getLevel(), 0);
            poseStack.popPose();
        }
    }
}
