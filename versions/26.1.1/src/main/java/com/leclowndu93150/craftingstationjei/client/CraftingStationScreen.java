package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import com.leclowndu93150.craftingstationjei.menu.SideContainerWrapper;
import com.leclowndu93150.craftingstationjei.network.C2SScrollPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> {

    private static final Identifier CRAFTING_TABLE_LOCATION =
            Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final Identifier SIDE_PANEL_TEXTURE =
            Identifier.fromNamespaceAndPath("craftingstation", "textures/gui/side_panel.png");
    public static final Identifier SECONDARY_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("craftingstation", "textures/gui/secondary.png");

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
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn) &&
                (!menu.hasSideContainers() || !isHovering(-126, -16, 126, 32 + imageHeight, mouseX, mouseY));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        extractBg(graphics, partialTicks, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (menu.hasSideContainers()) {
            Component displayName = menu.containerNames.getOrDefault(menu.getSelectedContainer(), Component.empty());
            String displayText = displayName.getString();
            String truncated = font.plainSubstrByWidth(displayText, 122);
            graphics.text(font, truncated, -122, 6, 0x404040, false);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.hasSideContainers()) {
            currentScroll = 0;
            return;
        }
        SideContainerWrapper handler = menu.getCurrentHandler();
        if (handler == null) {
            currentScroll = 0;
            return;
        }
        setScrollPos();
    }

    protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;

        if (this.menu.hasSideContainers()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SECONDARY_GUI_TEXTURE, i - 130, j, 0, 0, this.imageWidth, this.imageHeight + 18, 256, 256);

            int totalSlots = menu.getCurrentHandler() != null ? menu.getCurrentHandler().getSlotCount() : 0;
            int slotsToDraw = Math.min(totalSlots, VISIBLE_SLOTS);

            int offset = hasScrollbar() ? -126 : -118;

            for (int i3 = 0; i3 < slotsToDraw; i3++) {
                int j1 = i3 % 6;
                int k1 = i3 / 6;
                graphics.blit(RenderPipelines.GUI_TEXTURED, SIDE_PANEL_TEXTURE, i + j1 * 18 + offset, 18 * k1 + j + 16, 0, 0, 18, 18, 256, 256);
            }

            if (this.hasScrollbar()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, SIDE_PANEL_TEXTURE, i + SCROLLBAR_X, j + SCROLLBAR_Y, 20, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 256, 256);
                int k = (int) (j + SCROLLBAR_Y + 1 + SCROLLER_TRAVEL * currentScroll);
                graphics.blit(RenderPipelines.GUI_TEXTURED, SIDE_PANEL_TEXTURE, i + SCROLLBAR_X + 1, k, 40, 0, 12, SCROLLER_HEIGHT, 256, 256);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.hasScrollbar() && isHovering(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, event.x(), event.y())) {
            this.isScrolling = true;
            dragScrollbar(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isScrolling) {
            dragScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isScrolling = false;
        return super.mouseReleased(event);
    }

    private boolean hasScrollbar() {
        return menu.needsScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (this.hasScrollbar() && mouseX < leftPos && mouseX > leftPos - 20) {
            scrollMouse(scrollDeltaY);
            return true;
        }
        return false;
    }

    private void dragScrollbar(double mouseY) {
        currentScroll = Mth.clamp((mouseY - topPos - SCROLLBAR_Y - 1 - SCROLLER_HEIGHT / 2.0) / SCROLLER_TRAVEL, 0.0, 1.0);
        int maxRow = menu.getMaxFirstSlot() / SLOTS_PER_ROW;
        int previous = menu.getFirstSlot();
        menu.setFirstSlot((int) Math.round(currentScroll * maxRow) * SLOTS_PER_ROW);
        if (menu.getFirstSlot() != previous) {
            ClientPacketDistributor.sendToServer(new C2SScrollPacket(menu.getFirstSlot()));
        }
    }

    private void scrollMouse(double scrollDelta) {
        int firstSlot = (int) Mth.clamp(menu.getFirstSlot() - scrollDelta * SLOTS_PER_ROW, 0, menu.getMaxFirstSlot());
        menu.setFirstSlot(firstSlot);
        setScrollPos();
        ClientPacketDistributor.sendToServer(new C2SScrollPacket(menu.getFirstSlot()));
    }

    void setScrollPos() {
        int maxOffset = menu.getMaxFirstSlot();
        currentScroll = maxOffset <= 0 ? 0 : Mth.clamp((double) menu.getFirstSlot() / maxOffset, 0.0, 1.0);
    }
}
