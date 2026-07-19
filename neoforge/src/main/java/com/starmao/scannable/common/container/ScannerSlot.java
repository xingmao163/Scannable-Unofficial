package com.starmao.scannable.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that accepts only scanner module items and holds a maximum of one.
 * <p>Used in the scanner GUI to prevent non-module items from being placed
 * into the scanner's module storage.
 */
public final class ScannerSlot extends Slot {
    public ScannerSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return container.canPlaceItem(index, itemStack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
