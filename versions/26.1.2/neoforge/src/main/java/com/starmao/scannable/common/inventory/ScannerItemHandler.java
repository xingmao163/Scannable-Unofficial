package com.starmao.scannable.common.inventory;

import com.starmao.scannable.common.item.ScannerModuleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ResourceHandler} view over a {@link ScannerContainer}.
 *
 * <p>This is registered as the capability handler for the scanner item
 * via {@code Capabilities.Item.ITEM}. Each slot holds at most one
 * module item, and only {@link ScannerModuleItem} instances are accepted.
 */
public final class ScannerItemHandler implements ResourceHandler<ItemResource> {

    private final ScannerContainer container;

    public ScannerItemHandler(final ScannerContainer container) {
        this.container = container;
    }

    // ---- ResourceHandler<ItemResource> ---- //

    @Override
    public int size() {
        return container.getContainerSize();
    }

    @Override
    public ItemResource getResource(final int slot) {
        final ItemStack stack = container.getItem(slot);
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(final int slot) {
        return container.getItem(slot).isEmpty() ? 0 : 1;
    }

    @Override
    public long getCapacityAsLong(final int slot, final ItemResource resource) {
        return 1;
    }

    @Override
    public boolean isValid(final int slot, final ItemResource resource) {
        if (resource == null || resource.isEmpty()) return false;
        return resource.getItem() instanceof ScannerModuleItem
                && container.canPlaceItem(slot, resource.toStack(1));
    }

    /**
     * Inserts up to 1 unit of the given resource into the specified slot.
     *
     * @return the number of items actually inserted (0 or 1)
     */
    @Override
    public int insert(final int slot, final ItemResource resource, final int count,
                      final TransactionContext transaction) {
        if (resource == null || resource.isEmpty() || count <= 0) return 0;

        // Slot must be empty
        final ItemStack existing = container.getItem(slot);
        if (!existing.isEmpty()) return 0;

        // Must be a valid module item
        if (!isValid(slot, resource)) return 0;

        final int toInsert = Math.min(count, 1);

        // Perform the state change directly. Callers must manage transactions:
        // they create a Transaction, call insert(), then commit() or abort().
        container.setItem(slot, resource.toStack(toInsert));

        return toInsert;
    }

    /**
     * Extracts up to 1 unit of the given resource from the specified slot.
     *
     * @return the number of items actually extracted (0 or 1)
     */
    @Override
    public int extract(final int slot, final ItemResource resource, final int count,
                       final TransactionContext transaction) {
        if (count <= 0) return 0;

        final ItemStack existing = container.getItem(slot);
        if (existing.isEmpty()) return 0;

        // If a specific resource is requested, verify it matches
        if (resource != null && !resource.isEmpty() && !resource.matches(existing)) return 0;

        final int toExtract = Math.min(count, 1);

        // Perform the state change directly.
        container.setItem(slot, ItemStack.EMPTY);

        return toExtract;
    }

    // ---- Convenience (not part of ResourceHandler) ---- //

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
