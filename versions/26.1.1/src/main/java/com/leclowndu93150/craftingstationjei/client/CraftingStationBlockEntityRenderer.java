package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.blockentity.CraftingStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        for (int i = 0; i < state.items.size(); i++) {
            ItemStackRenderState itemState = state.items.get(i);
            if (itemState.isEmpty()) continue;
            poseStack.pushPose();
            float x = 0.22f + (i % 3) * 0.28f;
            float z = 0.22f + (i / 3) * 0.28f;
            poseStack.translate(x, 1.02, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.scale(0.22f, 0.22f, 0.22f);
            itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public static class RenderState extends BlockEntityRenderState {
        public List<ItemStackRenderState> items = Collections.emptyList();
    }
}
