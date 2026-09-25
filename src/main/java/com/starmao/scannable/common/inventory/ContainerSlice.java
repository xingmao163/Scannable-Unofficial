package com.starmao.scannable.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ContainerSlice implements Container {
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
    public ItemStack getItem(int i) {
        return container.getItem(offset + i);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        return container.removeItem(offset + i, j);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return container.removeItemNoUpdate(offset + i);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        container.setItem(offset + i, itemStack);
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
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return container.canPlaceItem(i, itemStack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < length; i++) {
            container.removeItemNoUpdate(offset + i);
        }
    }
}
