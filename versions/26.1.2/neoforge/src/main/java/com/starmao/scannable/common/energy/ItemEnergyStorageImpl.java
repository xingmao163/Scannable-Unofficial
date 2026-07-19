package com.starmao.scannable.common.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Optional;

/**
 * Implementation of {@link ItemEnergyStorage} that delegates to the NeoForge
 * {@link EnergyHandler} capability.
 * <p>Adapts the capability's long-based API for consistency.
 */
public final class ItemEnergyStorageImpl {
    /**
     * Wraps the NeoForge energy capability of the given stack in an
     * {@link ItemEnergyStorage} interface, if available.
     */
    public static Optional<ItemEnergyStorage> of(ItemStack container) {
        return Optional.ofNullable(container.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(container)))
                .map(capability -> new ItemEnergyStorage() {
                    @Override
                    public long receiveEnergy(long amount, boolean simulate) {
                        try (var tx = Transaction.openRoot()) {
                            int inserted = capability.insert((int) Math.min(amount, Integer.MAX_VALUE), tx);
                            if (!simulate) {
                                tx.commit();
                            }
                            return inserted;
                        }
                    }

                    @Override
                    public long extractEnergy(long amount, boolean simulate) {
                        try (var tx = Transaction.openRoot()) {
                            int extracted = capability.extract((int) Math.min(amount, Integer.MAX_VALUE), tx);
                            if (!simulate) {
                                tx.commit();
                            }
                            return extracted;
                        }
                    }

                    @Override
                    public long getEnergyStored() {
                        return capability.getAmountAsLong();
                    }

                    @Override
                    public long getMaxEnergyStored() {
                        return capability.getCapacityAsLong();
                    }
                });
    }

    private ItemEnergyStorageImpl() {
    }
}
