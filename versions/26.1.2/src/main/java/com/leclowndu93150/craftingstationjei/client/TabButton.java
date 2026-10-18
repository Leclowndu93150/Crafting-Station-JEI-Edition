package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class TabButton extends Button {

    private static final Identifier TABS_TEXTURE =
            Identifier.fromNamespaceAndPath("craftingstation", "textures/gui/tabs.png");

    private final Direction direction;
    private final CraftingStationMenu menu;

    public TabButton(int x, int y, int width, int height, OnPress onPress, Direction direction, CraftingStationMenu menu) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.direction = direction;
        this.menu = menu;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (menu.getSelectedContainer() == direction) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TABS_TEXTURE, getX(), getY(), 0, height, width, height, width, height * 2);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TABS_TEXTURE, getX(), getY(), 0, 0, width, height, width, height * 2);
        }
        ItemStack stack = menu.blocks.getOrDefault(direction, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            graphics.fakeItem(stack, getX() + 3, getY() + 3);
        }
    }
}
