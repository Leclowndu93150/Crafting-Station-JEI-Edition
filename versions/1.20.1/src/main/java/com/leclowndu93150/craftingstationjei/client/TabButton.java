package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TabButton extends Button {

    public final Direction direction;
    private final CraftingStationMenu craftingStationMenu;
    public static final ResourceLocation TAB = new ResourceLocation("craftingstationjei", "textures/gui/tabs.png");

    public TabButton(int x, int y, int widthIn, int heightIn, Button.OnPress callback, Direction direction, CraftingStationMenu craftingStationMenu) {
        super(x, y, widthIn, heightIn, Component.empty(), callback, DEFAULT_NARRATION);
        this.direction = direction;
        this.craftingStationMenu = craftingStationMenu;
    }

    @Override
    public void renderWidget(GuiGraphics matrices, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, TAB);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        if (craftingStationMenu.getSelectedContainer() == this.direction) {
            matrices.blit(TAB, getX(), getY(), 0, height, width, height, width, height * 2);
        } else {
            matrices.blit(TAB, getX(), getY(), 0, 0, width, height, width, height * 2);
        }
        ItemStack stack = craftingStationMenu.blocks.getOrDefault(direction, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            matrices.renderFakeItem(stack, getX() + 3, getY() + 3);
        }
    }
}
