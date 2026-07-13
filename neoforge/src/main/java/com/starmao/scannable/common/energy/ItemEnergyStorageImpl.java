package com.starmao.scannable.common.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Optional;

public final class ItemEnergyStorageImpl {
    public static Optional<ItemEnergyStorage> of(ItemStack container) {
        return Optional.ofNullable(container.getCapability(Capabilities.EnergyStorage.ITEM))
                .map(capability -> new ItemEnergyStorage() {
                    @Override
                    public long receiveEnergy(long amount, boolean simulate) {
                        return capability.receiveEnergy((int) Math.min(amount, Integer.MAX_VALUE), simulate);
                    }

                    @Override
                    public long extractEnergy(long amount, boolean simulate) {
                        return capability.extractEnergy((int) Math.min(amount, Integer.MAX_VALUE), simulate);
                    }

                    @Override
                    public long getEnergyStored() {
                        return capability.getEnergyStored();
                    }

                    @Override
                    public long getMaxEnergyStored() {
                        return capability.getMaxEnergyStored();
                    }
                });
    }

    private ItemEnergyStorageImpl() {
    }
}
