package com.starmao.scannable.common.container;

import com.starmao.scannable.common.item.ConfigurableModuleItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
/**
 * Base container menu for configurable scanner module configuration screens.
 * <p>Provides a standard player inventory layout and tracks the hand-held
 * module item. Subclasses implement {@link #removeItemAt} and
 * {@link #setItemAt} to modify the module's target list.
 */
public abstract class AbstractModuleContainerMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack stack;

    protected AbstractModuleContainerMenu(MenuType<?> type, int windowId, Inventory inventory, InteractionHand hand) {
        super(type, windowId);
        this.player = inventory.player;
        this.hand = hand;
        this.stack = player.getItemInHand(hand);

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, row * 18 + 51));
            }
        }
        for (int slot = 0; slot < 9; ++slot) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 109));
        }
    }

    public Player getPlayer() { return player; }
    public InteractionHand getHand() { return hand; }

    /**
     * Removes the configured target at the given index from the module item.
     * <p>Default implementation handles any {@link ConfigurableModuleItem}.
     * Subclasses may override for custom behaviour.
     */
    public void removeItemAt(int index) {
        ItemStack stack = getPlayer().getItemInHand(getHand());
        if (stack.getItem() instanceof ConfigurableModuleItem<?> item) {
            item.removeValueAt(stack, index);
        }
    }

    /** Sets a configured target at the given index on the module item. */
    public abstract void setItemAt(int index, ResourceLocation value);

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && ItemStack.matches(player.getItemInHand(hand), stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
