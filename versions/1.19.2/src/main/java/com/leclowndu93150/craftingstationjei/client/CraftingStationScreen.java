package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import com.leclowndu93150.craftingstationjei.network.C2SScrollPacket;
import com.leclowndu93150.craftingstationjei.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> {

    private static final ResourceLocation CRAFTING_TABLE_LOCATION =
            new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final ResourceLocation SIDE_PANEL_TEXTURE =
            new ResourceLocation("craftingstationjei", "textures/gui/side_panel.png");
    public static final ResourceLocation SECONDARY_GUI_TEXTURE =
            new ResourceLocation("craftingstationjei", "textures/gui/secondary.png");

    private static final int VISIBLE_ROWS = 9;
    private static final int SLOTS_PER_ROW = 6;
    public static final int VISIBLE_SLOTS = VISIBLE_ROWS * SLOTS_PER_ROW;

    private static final int SCROLLBAR_X = -17;
    private static final int SCROLLBAR_Y = 16;
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLLBAR_HEIGHT = 162;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int SCROLLER_TRAVEL = 145;

    private double currentScroll;
    private boolean isScrolling = false;

    public CraftingStationScreen(CraftingStationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        if (this.menu.hasSideContainers()) {
            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];
                if (menu.blockEntityMap.containsKey(direction)) {
                    addRenderableWidget(new TabButton(leftPos - 128 + 21 * i, topPos - 22, 22, 28,
                            button -> {
                                menu.setCurrentContainer(direction);
                                sendButtonToServer(CraftingStationMenu.ButtonAction.values()[direction.ordinal() + 1]);
                            }, direction, this.getMenu()));
                }
            }
        }
    }

    private void sendButtonToServer(CraftingStationMenu.ButtonAction action) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, action.ordinal());
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton) &&
                (!menu.hasSideContainers() || !isHovering(-126, -16, 126, 32 + imageHeight, mouseX, mouseY));
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
        super.renderLabels(stack, mouseX, mouseY);
        if (menu.hasSideContainers()) {
            Component displayName = menu.containerNames.getOrDefault(menu.getSelectedContainer(), Component.empty());
            String displayText = displayName.getString();
            String truncated = font.plainSubstrByWidth(displayText, 122);
            font.draw(stack, truncated, -122, 6, 0x404040);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.hasSideContainers()) {
            currentScroll = 0;
            return;
        }
        setScrollPos();
    }

    @Override
    protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION);
        this.blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;

        if (this.menu.hasSideContainers()) {
            RenderSystem.setShaderTexture(0, SECONDARY_GUI_TEXTURE);
            this.blit(stack, i - 130, j, 0, 0, this.imageWidth, this.imageHeight + 18);

            int totalSlots = menu.subContainerSize();
            int slotsToDraw = Math.min(totalSlots, VISIBLE_SLOTS);

            int offset = hasScrollbar() ? -126 : -118;

            RenderSystem.setShaderTexture(0, SIDE_PANEL_TEXTURE);
            for (int i3 = 0; i3 < slotsToDraw; i3++) {
                int j1 = i3 % 6;
                int k1 = i3 / 6;
                this.blit(stack, i + j1 * 18 + offset, 18 * k1 + j + 16, 0, 0, 18, 18);
            }

            if (this.hasScrollbar()) {
                this.blit(stack, i + SCROLLBAR_X, j + SCROLLBAR_Y, 20, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
                int k = (int) (j + SCROLLBAR_Y + 1 + SCROLLER_TRAVEL * currentScroll);
                this.blit(stack, i + SCROLLBAR_X + 1, k, 40, 0, 12, SCROLLER_HEIGHT);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int scroll) {
        if (this.hasScrollbar() && isHovering(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, mouseX, mouseY)) {
            this.isScrolling = true;
            dragScrollbar(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, scroll);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            dragScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int scroll) {
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, scroll);
    }

    private boolean hasScrollbar() {
        return menu.needsScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (this.hasScrollbar() && mouseX < leftPos && mouseX > leftPos - 20) {
            scrollMouse(scrollDelta);
            return true;
        }
        return false;
    }

    private void dragScrollbar(double mouseY) {
        currentScroll = Mth.clamp((mouseY - topPos - SCROLLBAR_Y - 1 - SCROLLER_HEIGHT / 2.0) / SCROLLER_TRAVEL, 0, 1);
        int maxRow = menu.getMaxFirstSlot() / SLOTS_PER_ROW;
        int previous = menu.getFirstSlot();
        menu.setFirstSlot((int) Math.round(currentScroll * maxRow) * SLOTS_PER_ROW);
        if (menu.getFirstSlot() != previous) {
            PacketHandler.CHANNEL.sendToServer(new C2SScrollPacket(menu.getFirstSlot()));
        }
    }

    private void scrollMouse(double scrollDelta) {
        int firstSlot = (int) Mth.clamp(menu.getFirstSlot() - scrollDelta * SLOTS_PER_ROW, 0, menu.getMaxFirstSlot());
        menu.setFirstSlot(firstSlot);
        setScrollPos();
        PacketHandler.CHANNEL.sendToServer(new C2SScrollPacket(menu.getFirstSlot()));
    }

    void setScrollPos() {
        int maxOffset = menu.getMaxFirstSlot();
        currentScroll = maxOffset <= 0 ? 0 : Mth.clamp((double) menu.getFirstSlot() / maxOffset, 0, 1);
    }
}
