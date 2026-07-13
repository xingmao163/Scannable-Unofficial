package com.starmao.scannable.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * A <strong>view</strong> over a contiguous sub-range of slots in a parent {@link Container}.
 *
 * <p>{@code ContainerSlice} does not own the items; it delegates every read and write
 * to the underlying container at an offset index. This is useful when a logical
 * container (e.g. "active modules", "inactive modules") is stored as a contiguous
 * segment of a larger physical backing container.
 *
 * <p><b>Example:</b> A scanner with 9 total slots (3 active + 6 inactive) exposes
 * two slices:
 * <pre>{@code
 * var active   = new ContainerSlice(fullContainer, 0, 3);  // slots 0..2
 * var inactive = new ContainerSlice(fullContainer, 3, 6);  // slots 3..8
 * }</pre>
 *
 * <p>Changes made through the slice propagate immediately to the parent, and
 * calling {@link #setChanged()} on the slice notifies the parent as well.
 *
 * @param <b>Note:</b> Index bounds are enforced by {@link #isIndexInBounds(int)};
 *               out-of-bounds reads return {@link ItemStack#EMPTY} and writes
 *               are silently dropped.
 */
public final class ContainerSlice implements Container, Iterable<ItemStack> {
    private final Container container;
    private final int offset;
    private final int length;

    /**
     * @param container The backing container that holds the actual items.
     * @param offset    Slot index in the parent where this slice begins (inclusive).
     * @param length    Number of slots this slice covers.
     */
    public ContainerSlice(Container container, int offset, int length) {
        this.container = container;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int getContainerSize() {
        return length;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < length; i++) {
            if (!container.getItem(offset + i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return isIndexInBounds(index) ? container.getItem(offset + index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return isIndexInBounds(index) ? container.removeItem(offset + index, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return isIndexInBounds(index) ? container.removeItemNoUpdate(offset + index) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (isIndexInBounds(index)) {
            container.setItem(offset + index, stack);
        }
    }

    @Override
    public void setChanged() {
        container.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void startOpen(Player player) {
        container.startOpen(player);
    }

    @Override
    public void stopOpen(Player player) {
        container.stopOpen(player);
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return container.canPlaceItem(i, itemStack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < length; i++) {
            container.removeItemNoUpdate(offset + i);
        }
        container.setChanged();
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < getContainerSize();
            }

            @Override
            public ItemStack next() {
                ItemStack stack = getItem(index);
                index++;
                return stack;
            }
        };
    }

    private boolean isIndexInBounds(int index) {
        return index >= 0 && index < length;
    }
}
