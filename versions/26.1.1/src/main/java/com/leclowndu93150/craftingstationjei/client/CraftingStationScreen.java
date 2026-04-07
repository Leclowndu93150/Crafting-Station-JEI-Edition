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
    private static final Identifier SCROLLBAR_BACKGROUND_AND_TAB =
            Identifier.parse("textures/gui/container/creative_inventory/tab_items.png");
    private static final Identifier SCROLLER_SPRITE =
            Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    public static final Identifier SECONDARY_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("craftingstation", "textures/gui/secondary.png");

    private static final int VISIBLE_ROWS = 9;
    private static final int SLOTS_PER_ROW = 6;
    public static final int VISIBLE_SLOTS = VISIBLE_ROWS * SLOTS_PER_ROW;

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
        int maxOffset = handler.getSlotCount() - VISIBLE_SLOTS;
        if (maxOffset <= 0) {
            currentScroll = 0;
        } else {
            currentScroll = (double) menu.getFirstSlot() / maxOffset;
        }
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
                graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_BACKGROUND_AND_TAB, i + j1 * 18 + offset, 18 * k1 + j + 16, 8, 17, 18, 18, 256, 256);
            }

            if (this.hasScrollbar()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_BACKGROUND_AND_TAB, i - 17, j + 16, 174, 17, 14, 100, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_BACKGROUND_AND_TAB, i - 17, j + 67, 174, 18, 14, 111, 256, 256);
                int k = (int) (j + 17 + 145 * currentScroll);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, i - 16, k, 12, 15);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.isScrolling = this.hasScrollbar();
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isScrolling) {
            int j = this.topPos;
            int j1 = j + 24;
            int j2 = j1 + 145;
            int k = this.leftPos;
            int k1 = k - 16;
            int k2 = k1 + 14;

            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX <= k2 && mouseX >= k1) {
                this.currentScroll = (mouseY - j1) / (j2 - j1 - 0f);
                currentScroll = Mth.clamp(currentScroll, 0.0, 1.0);
                scrollDrag(currentScroll);
            }
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

    private void scrollDrag(double scroll) {
        int firstSlot = (int) (scroll * (menu.subContainerSize() - VISIBLE_SLOTS));
        menu.setFirstSlot(firstSlot);
        ClientPacketDistributor.sendToServer(new C2SScrollPacket(firstSlot));
    }

    private void scrollMouse(double scrollDelta) {
        int firstSlot = (int) Mth.clamp(menu.getFirstSlot() - scrollDelta * SLOTS_PER_ROW, 0,
                menu.subContainerSize() - VISIBLE_SLOTS);
        menu.setFirstSlot(firstSlot);
        setScrollPos();
        ClientPacketDistributor.sendToServer(new C2SScrollPacket(firstSlot));
    }

    void setScrollPos() {
        double scroll = ((double) menu.getFirstSlot()) / (menu.subContainerSize() - VISIBLE_SLOTS);
        currentScroll = Mth.clamp(scroll, 0, 1);
    }
}
