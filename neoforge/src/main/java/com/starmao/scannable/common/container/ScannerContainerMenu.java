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
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, row * 18 + 77));
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
        Slot from = slots.get(index);
        ItemStack stack = from.getItem().copy();
        if (stack.isEmpty()) return ItemStack.EMPTY;

        boolean intoPlayerInventory = from.container != player.getInventory();
        ItemStack fromStack = from.getItem();

        int step, begin;
        if (intoPlayerInventory) {
            step = -1;
            begin = slots.size() - 1;
        } else {
            step = 1;
            begin = 0;
        }

        // Stack existing items
        if (fromStack.getMaxStackSize() > 1) {
            for (int i = begin; i >= 0 && i < slots.size(); i += step) {
                Slot into = slots.get(i);
                if (into.container == from.container) continue;

                ItemStack intoStack = into.getItem();
                if (intoStack.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(fromStack, intoStack)) continue;

                int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(stack));
                int spaceInSlot = maxSizeInSlot - intoStack.getCount();
                if (spaceInSlot <= 0) continue;

                int itemsMoved = Math.min(spaceInSlot, fromStack.getCount());
                if (itemsMoved <= 0) continue;

                intoStack.grow(from.remove(itemsMoved).getCount());
                into.setChanged();
                if (from.getItem().isEmpty()) break;
            }
        }

        // Move to empty slots
        for (int i = begin; i >= 0 && i < slots.size(); i += step) {
            if (from.getItem().isEmpty()) break;
            Slot into = slots.get(i);
            if (into.container == from.container) continue;
            if (into.hasItem()) continue;
            if (!into.mayPlace(fromStack)) continue;

            int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(fromStack));
            int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.set(from.remove(itemsMoved));
        }

        return from.getItem().getCount() < stack.getCount() ? from.getItem() : ItemStack.EMPTY;
    }
}
