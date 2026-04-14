package com.leclowndu93150.craftingstationjei.client;

import com.leclowndu93150.craftingstationjei.menu.CraftingStationMenu;
import com.leclowndu93150.craftingstationjei.menu.SideContainerWrapper;
import com.leclowndu93150.craftingstationjei.network.C2SScrollPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> {

    private static final ResourceLocation CRAFTING_TABLE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final ResourceLocation SCROLLBAR_BACKGROUND_AND_TAB =
            ResourceLocation.parse("textures/gui/container/creative_inventory/tab_items.png");
    private static final ResourceLocation SCROLLER_SPRITE =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    public static final ResourceLocation SECONDARY_GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("craftingstation", "textures/gui/secondary.png");

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
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton) &&
                (!menu.hasSideContainers() || !isHovering(-126, -16, 126, 32 + imageHeight, mouseX, mouseY));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics stack, int mouseX, int mouseY) {
        super.renderLabels(stack, mouseX, mouseY);
        if (menu.hasSideContainers()) {
            Component displayName = menu.containerNames.getOrDefault(menu.getSelectedContainer(), Component.empty());
            String displayText = displayName.getString();
            String truncated = font.plainSubstrByWidth(displayText, 122);
            stack.drawString(font, truncated, -122, 6, 0x404040, false);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.hasSideContainers()) {
            currentScroll = 0;
            return;
        }
        int totalSlots = menu.subContainerSize();
        int maxOffset = totalSlots - VISIBLE_SLOTS;
        if (maxOffset <= 0) {
            currentScroll = 0;
        } else {
            currentScroll = (double) menu.getFirstSlot() / maxOffset;
        }
    }

    @Override
    protected void renderBg(GuiGraphics stack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        stack.blit(CRAFTING_TABLE_LOCATION, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;

        if (this.menu.hasSideContainers()) {
            stack.blit(SECONDARY_GUI_TEXTURE, i - 130, j, 0, 0, this.imageWidth, this.imageHeight + 18);

            int totalSlots = menu.subContainerSize();
            int slotsToDraw = Math.min(totalSlots, VISIBLE_SLOTS);

            int offset = hasScrollbar() ? -126 : -118;

            for (int i3 = 0; i3 < slotsToDraw; i3++) {
                int j1 = i3 % 6;
                int k1 = i3 / 6;
                stack.blit(SCROLLBAR_BACKGROUND_AND_TAB, i + j1 * 18 + offset, 18 * k1 + j + 16, 8, 17, 18, 18);
            }

            if (this.hasScrollbar()) {
                stack.blit(SCROLLBAR_BACKGROUND_AND_TAB, i - 17, j + 16, 174, 17, 14, 100);
                stack.blit(SCROLLBAR_BACKGROUND_AND_TAB, i - 17, j + 67, 174, 18, 14, 111);
                int k = (int) (j + 17 + 145 * currentScroll);
                stack.blitSprite(SCROLLER_SPRITE, i - 16, k, 12, 15);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int scroll) {
        this.isScrolling = this.hasScrollbar();
        return super.mouseClicked(mouseX, mouseY, scroll);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int j = this.topPos;
            int j1 = j + 24;
            int j2 = j1 + 145;
            int k = this.leftPos;
            int k1 = k - 16;
            int k2 = k1 + 14;

            if (mouseX <= k2 && mouseX >= k1) {
                this.currentScroll = (mouseY - j1) / (j2 - j1 - 0f);
                currentScroll = Mth.clamp(currentScroll, 0, 1);
                scrollDrag(currentScroll);
            }
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
        PacketDistributor.sendToServer(new C2SScrollPacket(firstSlot));
    }

    private void scrollMouse(double scrollDelta) {
        int firstSlot = (int) Mth.clamp(menu.getFirstSlot() - scrollDelta * SLOTS_PER_ROW, 0,
                menu.subContainerSize() - VISIBLE_SLOTS);
        menu.setFirstSlot(firstSlot);
        setScrollPos();
        PacketDistributor.sendToServer(new C2SScrollPacket(firstSlot));
    }

    void setScrollPos() {
        double scroll = ((double) menu.getFirstSlot()) / (menu.subContainerSize() - VISIBLE_SLOTS);
        currentScroll = Mth.clamp(scroll, 0, 1);
    }
}
