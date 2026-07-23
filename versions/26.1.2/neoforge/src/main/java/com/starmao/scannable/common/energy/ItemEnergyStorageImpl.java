package com.starmao.scannable.common.energy;

import com.starmao.scannable.common.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public final class ItemEnergyStorageImpl {
    private ItemEnergyStorageImpl() {}

    public static Optional<ItemEnergyStorage> of(ItemStack container) {
        return Optional.of(new ItemEnergyStorage() {
            @Override public long getEnergyStored() { return container.getOrDefault(ModDataComponents.SCANNER_ENERGY.get(), 0); }
            @Override public long getMaxEnergyStored() { return com.starmao.scannable.common.config.ServerConfig.SCANNER_ENERGY_CAPACITY.get(); }
            @Override public long receiveEnergy(long amount, boolean simulate) {
                long current = getEnergyStored();
                long accepted = Math.min(amount, getMaxEnergyStored() - current);
                if (!simulate && accepted > 0) container.set(ModDataComponents.SCANNER_ENERGY.get(), (int)(current + accepted));
                return accepted;
            }
            @Override public long extractEnergy(long amount, boolean simulate) {
                long current = getEnergyStored();
                long extracted = Math.min(amount, current);
                if (!simulate && extracted > 0) container.set(ModDataComponents.SCANNER_ENERGY.get(), (int)(current - extracted));
                return extracted;
            }
        });
    }
}