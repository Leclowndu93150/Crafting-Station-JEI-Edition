package tfar.craftingstation;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

public class PersistantCraftingContainer implements CraftingContainer {

  private boolean doNotCallUpdates;
  private final AbstractContainerMenu eventHandler;
  protected final SimpleContainer inv;

  public PersistantCraftingContainer(AbstractContainerMenu eventHandler, SimpleContainer itemHandler) {
    this.eventHandler = eventHandler;
    this.inv = itemHandler;
    doNotCallUpdates = false;
  }

  @Override
  public int getWidth() {
    return 3;
  }

  @Override
  public int getHeight() {
    return 3;
  }

  /**
   * Returns the stack in this slot.  This stack should be a modifiable reference, not a copy of a stack in your inventory.
   */
  @Override
  public ItemStack getItem(int slot) {
    validate(slot);
    return inv.getItem(slot);
  }

  public void validate(int slot){
    if (isValid(slot))return;
    throw new IndexOutOfBoundsException("Someone attempted to poll an outofbounds stack at slot " +
            slot+" report to them, NOT Crafting Station");
  }

  public boolean isValid(int slot){
    return slot >= 0 && slot < getContainerSize();
  }

  /**
   * Attempts to remove n items from the specified slot.  Returns the split stack that was removed.  Modifies the inventory.
   */
  @Override
  public ItemStack removeItem(int slot, int count) {
    validate(slot);
    ItemStack stack = inv.removeItem(slot,count);
    if (!stack.isEmpty())
      onCraftMatrixChanged();
    return stack;
  }

  /**
   * Sets the contents of this slot to the provided stack.
   */
  @Override
  public void setItem(int slot,ItemStack stack) {
    validate(slot);
    inv.setItem(slot, stack);
    onCraftMatrixChanged();
  }

  /**
   * Removes the stack contained in this slot from the underlying handler, and returns it.
   */
  @Override
  public ItemStack removeItemNoUpdate(int index) {
    validate(index);
    ItemStack s = getItem(index);
    if(s.isEmpty()) return ItemStack.EMPTY;
    onCraftMatrixChanged();
    setItem(index, ItemStack.EMPTY);
    return s;
  }

  @Override
  public NonNullList<ItemStack> getItems(){
    return inv.items;
  }

  @Override
  public boolean isEmpty() {
    return inv.isEmpty();
  }

  @Override
  public int getContainerSize() {
    return inv.getContainerSize();
  }

  @Override
  public void clearContent(){
    //dont
  }

  @Override
  public void setChanged() {
    onCraftMatrixChanged();
  }

  @Override
  public boolean stillValid(Player player) {
    return eventHandler.stillValid(player);
  }

  /**
   * If set to true no eventhandler.onCraftMatrixChanged calls will be made.
   * This is used to prevent recipe check when changing the item slots when something is crafted
   * (since each slot with an item is reduced by 1, it changes -> callback)
   */
  public void setDoNotCallUpdates(boolean doNotCallUpdates) {
    this.doNotCallUpdates = doNotCallUpdates;
  }

  @Override
  public void fillStackedContents(StackedContents stackedContents) {

  }

  public void onCraftMatrixChanged() {
    if(!doNotCallUpdates) {
      this.eventHandler.slotsChanged(this);
    }
  }
}