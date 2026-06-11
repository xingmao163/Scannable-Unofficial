package com.starmao.scannable.client.scanning;

public final class ProviderCacheManager {
    public static void clearCache() {
        // Reset scan result provider caches so results are re-baked with new color settings.
        ScanResultProviders.BLOCKS.get().reset();
        ScanResultProviders.ENTITIES.get().reset();
        com.starmao.scannable.common.scanning.FluidBlockScannerModule.clearCache();
    }

    private ProviderCacheManager() {
    }
}
