package com.starmao.scannable.common.inventory;

import com.starmao.scannable.common.item.ScannerModuleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link IItemHandler} view over a {@link ScannerContainer}.
 *
 * <p>This is registered as the {@code Capabilities.ItemHandler.ITEM} capability
 * for the scanner item, replacing the generic {@link net.neoforged.neoforge.items.wrapper.InvWrapper}
 * that was previously used. Benefits over {@code InvWrapper}:
 *
 * <ul>
 *   <li><b>Slot validation</b> — prevents non-module items from being inserted
 *       into scanner storage, preserving the invariant that every slot in a
 *       scanner {@link ItemStack} holds a valid module item.
 *   <li><b>Clear intent</b> — a named class documents the fact that the scanner
 *       exposes its internal module inventory to hoppers, other mods, and
 *       player interactions.
 *   <li><b>Better logging / debuggability</b> — distinct handler in stack traces.
 * </ul>
 */
public final class ScannerItemHandler implements IItemHandlerModifiable {

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

    // ---- IItemHandlerModifiable ---- //

    @Override
    public void setStackInSlot(final int slot, final @NotNull ItemStack stack) {
        container.setItem(slot, stack);
    }

    // ---- Object ---- //

    @Override
    public String toString() {
        return "ScannerItemHandler{" + container + "}";
    }
}
