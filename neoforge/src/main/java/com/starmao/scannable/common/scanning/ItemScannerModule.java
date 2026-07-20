package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Scanner module that detects specific items inside nearby containers.
 * <p>Unlike block/entity scanning, item scanning runs on the server side
 * where container inventories are fully accessible. Results are sent
 * to the client via {@link com.starmao.scannable.common.network.message.S2CItemScanResult}.
 * <p>Singleton enum — stateless.
 */
public enum ItemScannerModule implements ScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_ITEM.get();
    }

    @Override
    public boolean hasResultProvider() {
        return false; // Item scanning is server-driven, not provider-based
    }

    @Nullable
    @Override
    public ScanResultProvider getResultProvider() {
        return null;
    }
}
