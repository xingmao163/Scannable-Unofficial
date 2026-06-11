package com.starmao.scannable.common.energy;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface ItemEnergyStorage {
    static Optional<ItemEnergyStorage> of(ItemStack stack) {
        return ItemEnergyStorageImpl.of(stack);
    }

    long receiveEnergy(long amount, boolean simulate);

    long extractEnergy(long amount, boolean simulate);

    long getEnergyStored();

    long getMaxEnergyStored();
}
