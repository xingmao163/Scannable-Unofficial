package com.starmao.scannable.common.energy;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Abstraction over an item's energy storage, wrapping the NeoForge capability
 * system behind a cleaner long-based interface.
 */
public interface ItemEnergyStorage {
    /** Resolves the energy storage for the given item stack, if available. */
    static Optional<ItemEnergyStorage> of(ItemStack stack) {
        return ItemEnergyStorageImpl.of(stack);
    }

    /** Adds energy to the storage. Returns the amount actually received. */
    long receiveEnergy(long amount, boolean simulate);

    /** Removes energy from the storage. Returns the amount actually extracted. */
    long extractEnergy(long amount, boolean simulate);

    /** @return the energy currently stored */
    long getEnergyStored();

    /** @return the maximum energy capacity */
    long getMaxEnergyStored();
}
