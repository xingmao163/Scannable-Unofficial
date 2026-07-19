package com.starmao.scannable.common.energy.neoforge;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Energy storage implementation for the scanner item using the 26.1.2 transfer API.
 *
 * <p>Wraps the energy stored in the scanner's {@link ModDataComponents#SCANNER_ENERGY}
 * data component via {@link ItemAccessEnergyHandler}, which implements the
 * {@link net.neoforged.neoforge.transfer.energy.EnergyHandler} interface required
 * by {@code Capabilities.Energy.ITEM}.
 */
public final class ScannerEnergyStorage extends ItemAccessEnergyHandler {

    public ScannerEnergyStorage(ItemStack container) {
        super(ItemAccess.forStack(container), ModDataComponents.SCANNER_ENERGY.get(), ModConfig.SCANNER_ENERGY_CAPACITY.get());
    }

    public static ScannerEnergyStorage of(ItemStack container) {
        return new ScannerEnergyStorage(container);
    }

    @Override
    public int insert(int maxInsert, TransactionContext transaction) {
        if (!ModConfig.SCANNER_USE_ENERGY.get()) return 0;
        return super.insert(maxInsert, transaction);
    }

    @Override
    public int extract(int maxExtract, TransactionContext transaction) {
        if (!ModConfig.SCANNER_USE_ENERGY.get()) return 0;
        return super.extract(maxExtract, transaction);
    }

}
