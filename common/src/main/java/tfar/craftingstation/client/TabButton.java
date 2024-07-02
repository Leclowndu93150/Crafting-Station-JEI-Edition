package tfar.craftingstation.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tfar.craftingstation.CraftingStation;
import tfar.craftingstation.menu.CraftingStationMenu;

public class TabButton extends Button{

  public final Direction direction;
    private final CraftingStationMenu craftingStationMenu;

    public TabButton(int x, int y, int widthIn, int heightIn, Button.OnPress callback, Direction direction, CraftingStationMenu craftingStationMenu) {
    super(x, y, widthIn, heightIn, Component.empty(), callback,DEFAULT_NARRATION);
    this.direction = direction;
        this.craftingStationMenu = craftingStationMenu;
    }
  public static final ResourceLocation TAB = CraftingStation.id("textures/gui/tabs.png");


  @Override
  public void renderWidget(GuiGraphics matrices, int mouseX, int mouseY, float partialTicks) {
      RenderSystem.setShaderTexture(0,TAB);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      if (true) {
        matrices.blit(TAB,getX(), getY(), 0, height, width, height,width,height * 2);
      } else {
        matrices.blit(TAB, getX(), getY(), 0, 0, width, height,width,height * 2);
      }
      ItemStack stack = craftingStationMenu.blocks.getOrDefault(direction,ItemStack.EMPTY);
      if (!stack.isEmpty()) {
        matrices.renderFakeItem(stack, getX() +3, getY() +3);
      }
  }
}

