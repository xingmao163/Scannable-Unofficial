package com.starmao.scannable.common.energy.neoforge;

import com.starmao.scannable.common.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Energy handler implementation for the scanner item using NeoForge's transfer API.
 * Stores FE energy in a data component on the item stack.
 */
public final class ScannerEnergyStorage implements EnergyHandler {
    private final ItemStack container;

    private ScannerEnergyStorage(ItemStack container) {
        this.container = container;
    }

    public static EnergyHandler of(ItemStack stack) {
        return new ScannerEnergyStorage(stack);
    }

    @Override
    public long getAmountAsLong() {
        return container.getOrDefault(ModDataComponents.SCANNER_ENERGY.get(), 0);
    }

    @Override
    public long getCapacityAsLong() {
        return com.starmao.scannable.common.config.ServerConfig.SCANNER_ENERGY_CAPACITY.get();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        int current = (int) getAmountAsLong();
        int capacity = (int) getCapacityAsLong();
        int accepted = Math.min(amount, capacity - current);
        if (accepted > 0) {
            container.set(ModDataComponents.SCANNER_ENERGY.get(), current + accepted);
        }
        return accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        int current = (int) getAmountAsLong();
        int extracted = Math.min(amount, current);
        if (extracted > 0) {
            container.set(ModDataComponents.SCANNER_ENERGY.get(), current - extracted);
        }
        return extracted;
    }
}
