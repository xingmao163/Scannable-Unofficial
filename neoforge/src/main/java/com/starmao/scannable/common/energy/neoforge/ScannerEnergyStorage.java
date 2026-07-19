package com.starmao.scannable.common.energy.neoforge;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * NeoForge {@link EnergyStorage} implementation for the scanner item.
 * <p>Persists energy changes to the item's {@link ModDataComponents#SCANNER_ENERGY}
 * data component so energy survives item serialisation.
 * Capacity is read from {@link ModConfig#SCANNER_ENERGY_CAPACITY}.
 */
public final class ScannerEnergyStorage extends EnergyStorage {
    private final ItemStack container;

    public ScannerEnergyStorage(ItemStack container) {
        super(ModConfig.SCANNER_ENERGY_CAPACITY.get());
        this.container = container;
        this.energy = Math.max(0, Math.min(capacity, container.getOrDefault(ModDataComponents.SCANNER_ENERGY.get(), 0)));
    }

    /** Creates a ScannerEnergyStorage for the given item stack. */
    public static ScannerEnergyStorage of(ItemStack container) {
        return new ScannerEnergyStorage(container);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = super.receiveEnergy(maxReceive, simulate);
        if (!simulate && energyReceived != 0) {
            container.set(ModDataComponents.SCANNER_ENERGY.get(), this.energy);
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = super.extractEnergy(maxExtract, simulate);
        if (!simulate && energyExtracted != 0) {
            container.set(ModDataComponents.SCANNER_ENERGY.get(), this.energy);
        }
        return energyExtracted;
    }
}
