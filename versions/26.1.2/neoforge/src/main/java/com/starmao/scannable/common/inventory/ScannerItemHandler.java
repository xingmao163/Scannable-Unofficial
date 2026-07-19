package com.starmao.scannable.common.inventory;

import com.starmao.scannable.common.item.ScannerModuleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link IItemHandler} view over a {@link ScannerContainer}.
 *
 * <p>This is registered as the capability handler for the scanner item.
 * Slot validation prevents non-module items from being inserted into scanner storage.
 *
 * <p>NOTE: In 26.1, {@code IItemHandlerModifiable} is deprecated for removal,
 * so this handler implements only {@link IItemHandler}. The
 * {@link #setStackInSlot} method is retained as a public convenience but
 * is not part of the capability interface.
 */
public final class ScannerItemHandler implements IItemHandler {

    private final ScannerContainer container;

    public ScannerItemHandler(final ScannerContainer container) {
        this.container = container;
    }

    // ---- IItemHandler ---- //

    @Override
    public int getSlots() {
        return container.getContainerSize();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(final int slot) {
        return container.getItem(slot);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(final int slot, final @NotNull ItemStack stack) {
        return stack.getItem() instanceof ScannerModuleItem && container.canPlaceItem(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(final int slot, final @NotNull ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, stack)) return stack;

        final ItemStack existing = container.getItem(slot);
        if (!existing.isEmpty()) return stack; // each slot holds at most 1 item

        if (!simulate) {
            container.setItem(slot, stack.copyWithCount(1));
        }
        final ItemStack remainder = stack.copy();
        remainder.shrink(1);
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        final ItemStack existing = container.getItem(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        if (!simulate) {
            container.setItem(slot, ItemStack.EMPTY);
        }
        return existing.copyWithCount(1);
    }

    // ---- Non-interface convenience (was IItemHandlerModifiable) ---- //

    /**
     * Directly sets the stack in the given slot without validation.
     * Retained for internal use where the caller has already validated.
     */
    public void setStackInSlot(final int slot, final @NotNull ItemStack stack) {
        container.setItem(slot, stack);
    }

    // ---- Object ---- //

    @Override
    public String toString() {
        return "ScannerItemHandler{" + container + "}";
    }
}
