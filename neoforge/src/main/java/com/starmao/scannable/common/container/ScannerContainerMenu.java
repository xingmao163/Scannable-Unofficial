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
        final Slot from = slots.get(index);
        final ItemStack stack = from.getItem().copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final boolean intoPlayerInventory = from.container != player.getInventory();
        final ItemStack fromStack = from.getItem();

        final int step, begin;
        if (intoPlayerInventory) {
            step = -1;
            begin = slots.size() - 1;
        } else {
            step = 1;
            begin = 0;
        }

        // Phase 1: merge with existing matching stacks (for stackable items only)
        if (fromStack.getMaxStackSize() > 1) {
            for (int i = begin; i >= 0 && i < slots.size(); i += step) {
                final Slot into = slots.get(i);
                if (into.container == from.container) {
                    continue;
                }

                final ItemStack intoStack = into.getItem();
                if (intoStack.isEmpty()) {
                    continue;
                }

                if (!ItemStack.isSameItemSameComponents(fromStack, intoStack)) {
                    continue;
                }

                final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(stack));
                final int spaceInSlot = maxSizeInSlot - intoStack.getCount();
                if (spaceInSlot <= 0) {
                    continue;
                }

                final int itemsMoved = Math.min(spaceInSlot, fromStack.getCount());
                if (itemsMoved <= 0) {
                    continue;
                }

                intoStack.grow(from.remove(itemsMoved).getCount());
                into.setChanged();

                if (from.getItem().isEmpty()) {
                    break;
                }
            }
        }

        // Phase 2: move to empty slots
        for (int i = begin; i >= 0 && i < slots.size(); i += step) {
            if (from.getItem().isEmpty()) {
                break;
            }

            final Slot into = slots.get(i);
            if (into.container == from.container) {
                continue;
            }

            if (into.hasItem()) {
                continue;
            }

            if (!into.mayPlace(fromStack)) {
                continue;
            }

            final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(fromStack));
            final int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.set(from.remove(itemsMoved));
        }

        return from.getItem().getCount() < stack.getCount() ? from.getItem() : ItemStack.EMPTY;
    }
}
