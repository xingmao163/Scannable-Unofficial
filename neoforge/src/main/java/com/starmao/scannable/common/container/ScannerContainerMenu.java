package com.starmao.scannable.common.container;

import com.starmao.scannable.common.inventory.ScannerContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the main scanner GUI.
 * <p>Layout: 3 active module slots, 6 inactive module slots, plus the player's
 * inventory and hotbar. The scanner item itself is protected from being moved
 * into its own slots.
 */
public final class ScannerContainerMenu extends AbstractContainerMenu {
    public static ScannerContainerMenu create(int windowId, Inventory inventory, FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new ScannerContainerMenu(windowId, inventory, hand,
                new ScannerContainer(inventory.player.getItemInHand(hand)));
    }

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack stack;

    public ScannerContainerMenu(int windowId, Inventory inventory, InteractionHand hand, ScannerContainer itemHandler) {
        super(ModMenus.SCANNER.get(), windowId);

        this.player = inventory.player;
        this.hand = hand;
        this.stack = player.getItemInHand(hand);

        // Active module slots (3)
        Container activeModules = itemHandler.getActiveModules();
        for (int slot = 0; slot < activeModules.getContainerSize(); ++slot) {
            addSlot(new ScannerSlot(activeModules, slot, 62 + slot * 18, 20));
        }

        // Inactive module slots (6)
        Container storedModules = itemHandler.getInactiveModules();
        for (int slot = 0; slot < storedModules.getContainerSize(); ++slot) {
            addSlot(new ScannerSlot(storedModules, slot, 62 + slot * 18, 46));
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int slot = 0; slot < 9; ++slot) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 135));
        }
    }

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && ItemStack.matches(player.getItemInHand(hand), stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        // If from scanner module area, move to inventory
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else {
            // If from inventory, try to move to scanner
            if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return result;
    }
}
