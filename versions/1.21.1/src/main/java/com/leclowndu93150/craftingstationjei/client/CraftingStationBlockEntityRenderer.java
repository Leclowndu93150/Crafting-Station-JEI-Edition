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

    public CraftingStationBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(CraftingStationBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = be.getInput().getItem(i);
            if (stack.isEmpty()) continue;
            poseStack.pushPose();
            float x = 0.22f + (i % 3) * 0.28f;
            float z = 0.22f + (i / 3) * 0.28f;
            poseStack.translate(x, 1.02, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.scale(0.22f, 0.22f, 0.22f);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, buffer, be.getLevel(), 0);
            poseStack.popPose();
        }
    }
}
