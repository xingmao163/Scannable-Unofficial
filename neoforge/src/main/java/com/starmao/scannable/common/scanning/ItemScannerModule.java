package com.starmao.scannable.common.scanning;

import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.config.ModConfig;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Scanner module that searches for configured items inside containers within range.
 *
 * <p>Scans all container blocks and checks their inventory contents against a
 * user-configured list of target items. Matching containers are highlighted
 * with the found item's name and quantity displayed.
 */
public enum ItemScannerModule implements ScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_ITEM.get();
    }

    @Nullable
    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("items");
    }
}
