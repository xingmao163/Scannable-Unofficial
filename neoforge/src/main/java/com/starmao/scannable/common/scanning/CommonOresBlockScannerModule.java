package com.starmao.scannable.common.scanning;

import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.config.ConfigParsers;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.scanning.filter.BlockCacheScanFilter;
import com.starmao.scannable.common.scanning.filter.BlockScanFilter;
import com.starmao.scannable.common.scanning.filter.BlockTagScanFilter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Scanner module that detects common ore blocks (coal, iron, copper, gold, lapis, redstone).
 *
 * <p>Which blocks are considered "common ores" is controlled by the
 * {@link ModConfig#COMMON_ORE_TAGS} and {@link ModConfig#COMMON_ORE_BLOCKS}
 * configuration entries. The filter is lazily built and cached; call
 * {@link #clearCache()} when config changes are applied.
 *
 * <p>Singleton enum — stateless.
 */
public enum CommonOresBlockScannerModule implements BlockScannerModule {
    INSTANCE;

    private Predicate<BlockState> filter;

    /** Clears the cached filter so it is rebuilt on the next scan. */
    public static void clearCache() {
        INSTANCE.filter = null;
    }

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_ORE_COMMON.get();
    }

    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("blocks");
    }

    @Override
    public float adjustLocalRange(float range) {
        return range * (float) (double) ModConfig.SCANNER_RANGE_MODIFIER_ORE_COMMON.get();
    }

    @Override
    public Predicate<BlockState> getFilter(ItemStack module) {
        validateFilter();
        return filter;
    }

    private void validateFilter() {
        if (filter != null) return;

        List<Predicate<BlockState>> filters = new ArrayList<>();

        // Specific block IDs
        for (final Block block : ConfigParsers.parseBlocks(ModConfig.COMMON_ORE_BLOCKS.get())) {
            filters.add(new BlockScanFilter(block));
        }

        // Block tags
        for (final TagKey<Block> tag : ConfigParsers.parseBlockTags(ModConfig.COMMON_ORE_TAGS.get())) {
            filters.add(new BlockTagScanFilter(tag));
        }

        if (filters.isEmpty()) {
            filter = state -> false;
        } else {
            filter = new BlockCacheScanFilter(filters);
        }
    }
}
