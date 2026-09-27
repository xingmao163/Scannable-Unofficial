package com.starmao.scannable.common.inventory;

import com.starmao.scannable.common.item.ScannerModuleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A {@link ResourceHandler} view over a {@link ScannerContainer}, exposed as the
 * {@code Capabilities.Item.ITEM} capability of the scanner item.
 *
 * <p>This is the 26.1 replacement for the old {@code IItemHandler}-based wrapper
 * ({@code InvWrapper} / {@code IItemHandlerModifiable}). It keeps the same
 * guarantees the older handler had:
 *
 * <ul>
 *   <li><b>Slot validation</b> — only {@link ScannerModuleItem}s may be inserted,
 *       preserving the invariant that every scanner slot holds a valid module.
 *   <li><b>One item per slot</b> — capacity is 1 for every slot.
 *   <li><b>Transaction safety</b> — mutations are journalled via
 *       {@link SnapshotJournal}, so a rolled-back transaction restores the previous
 *       contents instead of leaking partial changes.
 * </ul>
 */
public final class ScannerItemHandler extends SnapshotJournal<ItemStack[]>
        implements ResourceHandler<ItemResource> {

    private final ScannerContainer container;

    public ScannerItemHandler(final ScannerContainer container) {
        this.container = container;
    }

    // ---- ResourceHandler ---- //

    @Override
    public int size() {
        return container.getContainerSize();
    }

    @Override
    public ItemResource getResource(final int index) {
        final ItemStack stack = container.getItem(index);
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(final int index) {
        return container.getItem(index).getCount();
    }

    @Override
    public long getCapacityAsLong(final int index, final ItemResource resource) {
        // One module per slot; report 0 for resources this slot would reject.
        if (resource != null && !resource.isEmpty() && !isValid(index, resource)) return 0;
        return 1;
    }

    @Override
    public boolean isValid(final int index, final ItemResource resource) {
        if (index < 0 || index >= size()) return false;
        if (resource == null || resource.isEmpty()) return false;
        if (!(resource.getItem() instanceof ScannerModuleItem)) return false;
        return container.canPlaceItem(index, resource.toStack(1));
    }

    @Override
    public int insert(final int index, final ItemResource resource, final int amount,
                      final TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty() || !isValid(index, resource)) return 0;

        // Each slot holds at most a single module.
        if (!container.getItem(index).isEmpty()) return 0;

        updateSnapshots(transaction);
        // Bypass the container's module check: isValid() already verified the resource,
        // and the checked setItem() would also reject the empty stacks used when clearing.
        container.setItemUnchecked(index, resource.toStack(1));
        // Must respect the per-slot capacity of 1: ResourceHandler#insert(resource, amount, tx)
        // walks every slot passing `amount - inserted`, so returning `amount` here would both
        // over-report and let a caller believe it moved more than one item per slot.
        return 1;
    }

    @Override
    public int extract(final int index, final ItemResource resource, final int amount,
                       final TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) return 0;

        final ItemStack existing = container.getItem(index);
        if (existing.isEmpty() || !resource.matches(existing)) return 0;

        updateSnapshots(transaction);
        // Must go through the unchecked path: the checked setItem() rejects ItemStack.EMPTY,
        // so extracting through it would silently do nothing.
        container.setItemUnchecked(index, ItemStack.EMPTY);
        // Never report more than was asked for (the contract requires [0, amount]).
        return Math.min(amount, existing.getCount());
    }

    // ---- SnapshotJournal ---- //

    /** @return a defensive copy of every slot, used to roll this handler back. */
    @Override
    protected ItemStack[] createSnapshot() {
        final int slots = size();
        final ItemStack[] snapshot = new ItemStack[slots];
        for (int i = 0; i < slots; i++) {
            snapshot[i] = container.getItem(i).copy();
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(final ItemStack[] snapshot) {
        for (int i = 0; i < snapshot.length; i++) {
            // Unchecked: rolling back to an empty slot has to clear it, and the checked
            // setItem() would refuse the empty stack, leaving the aborted change in place.
            container.setItemUnchecked(i, snapshot[i]);
        }
    }

    @Override
    protected void onRootCommit(final ItemStack[] originalState) {
        container.setChanged();
    }

    // ---- Object ---- //

    @Override
    public String toString() {
        return "ScannerItemHandler{" + container + "}";
    }
}
