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

    private static final ResourceLocation TABS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("craftingstation", "textures/gui/tabs.png");

    private final Direction direction;
    private final CraftingStationMenu menu;

    public TabButton(int x, int y, int width, int height, OnPress onPress, Direction direction, CraftingStationMenu menu) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.direction = direction;
        this.menu = menu;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderTexture(0, TABS_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        if (menu.getSelectedContainer() == direction) {
            graphics.blit(TABS_TEXTURE, getX(), getY(), 0, height, width, height, width, height * 2);
        } else {
            graphics.blit(TABS_TEXTURE, getX(), getY(), 0, 0, width, height, width, height * 2);
        }
        ItemStack stack = menu.blocks.getOrDefault(direction, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, getX() + 3, getY() + 3);
        }
    }
}
