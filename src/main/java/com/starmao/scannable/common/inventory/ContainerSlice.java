package com.starmao.scannable.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public final class ContainerSlice implements Container, Iterable<ItemStack> {
    private final Container container;
    private final int offset;
    private final int length;

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
