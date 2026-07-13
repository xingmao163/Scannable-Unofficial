package com.starmao.scannable.api;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Interface for a scanner module.
 * <p>
 * Implement this to create a module that can be installed in the scanner.
 * Scanning behavior is <em>client side only</em>; only getEnergyCost and
 * hasResultProvider are called on the server.
 */
public interface ScannerModule {
    int getEnergyCost(ItemStack module);

    default boolean hasResultProvider() {
        return true;
    }

    @Nullable
    ScanResultProvider getResultProvider();

    default float adjustGlobalRange(float range) {
        return range;
    }
}
