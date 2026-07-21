package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ServerConfig;

import java.util.List;

/**
 * Scanner module that detects common ore blocks (coal, iron, copper, gold, lapis, redstone).
 *
 * <p>Which blocks are considered "common ores" is controlled by the
 * {@link ServerConfig#COMMON_ORE_TAGS} and {@link ServerConfig#COMMON_ORE_BLOCKS}
 * configuration entries. The filter is lazily built and cached.
 *
 * <p>Singleton — use {@link #INSTANCE}.
 */
public final class CommonOresBlockScannerModule extends AbstractOreBlockScannerModule {
    public static final CommonOresBlockScannerModule INSTANCE = new CommonOresBlockScannerModule();

    private CommonOresBlockScannerModule() {}

    /** Clears the cached filter so it is rebuilt on the next scan. */
    public static void clearCache() {
        INSTANCE.clearFilter();
    }

    @Override
    protected int getEnergyCostConfig() {
        return ServerConfig.SCANNER_ENERGY_COST_ORE_COMMON.get();
    }

    @Override
    protected float getRangeModifierConfig() {
        return ServerConfig.SCANNER_RANGE_MODIFIER_ORE_COMMON.get().floatValue();
    }

    @Override
    protected List<? extends String> getBlockConfig() {
        return ServerConfig.COMMON_ORE_BLOCKS.get();
    }

    @Override
    protected List<? extends String> getTagConfig() {
        return ServerConfig.COMMON_ORE_TAGS.get();
    }
}
