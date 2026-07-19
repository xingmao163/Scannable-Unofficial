package com.starmao.scannable.common.scanning;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Scanner module that handles the charging phase of the scanning process.
 * <p>This module itself has no {@link ScanResultProvider} and provides no
 * scan results — it only contributes energy cost during the charging phase.
 * The actual scanning is initiated by the active modules in the scanner.
 * <p>Singleton enum — stateless.
 */
public enum ChargingScannerModule implements ScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return 0; // charging itself doesn't cost energy
    }

    @Override
    public boolean hasResultProvider() {
        return false;
    }

    @Nullable
    @Override
    public ScanResultProvider getResultProvider() {
        return null;
    }
}
