package com.starmao.scannable.common.scanning;

import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.config.ConfigParsers;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.scanning.filter.BlockCacheScanFilter;
import com.starmao.scannable.common.scanning.filter.BlockScanFilter;
import com.starmao.scannable.common.scanning.filter.BlockTagScanFilter;
import com.starmao.scannable.common.scanning.filter.IgnoredBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Scanner module that detects rare ore blocks (diamond, emerald, netherite, quartz, etc.).
 *
 * <p>By default, any block tagged with the configured top-level ore tag
 * ({@link ModConfig#RARE_ORE_TOP_TAG}, default {@code c:ores}) is considered
 * a rare ore — unless it is already caught by the
 * {@link CommonOresBlockScannerModule} (common ores) or is in the
 * {@link IgnoredBlocks} list. Players may also add extra block IDs via
 * {@link ModConfig#RARE_ORE_BLOCKS} and extra tags via
 * {@link ModConfig#RARE_ORE_TAGS}.
 *
 * <p>The filter is lazily built and cached; call {@link #clearCache()}
 * when config changes are applied.
 *
 * <p>Singleton enum — stateless.
 */
public enum RareOresBlockScannerModule implements BlockScannerModule {
    INSTANCE;

    private Predicate<BlockState> filter;

    /** Clears the cached filter so it is rebuilt on the next scan. */
    public static void clearCache() {
        INSTANCE.filter = null;
    }

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_ORE_RARE.get();
    }

    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("blocks");
    }

    @Override
    public float adjustLocalRange(float range) {
        return range * (float) (double) ModConfig.SCANNER_RANGE_MODIFIER_ORE_RARE.get();
    }

    @Override
    public Predicate<BlockState> getFilter(ItemStack module) {
        validateFilter();
        return filter;
    }

    private void validateFilter() {
        if (filter != null) return;

        List<Predicate<BlockState>> filters = new ArrayList<>();

        // Extra rare block IDs (beyond the implicit top-level-ore-tag rule)
        for (final Block block : ConfigParsers.parseBlocks(ModConfig.RARE_ORE_BLOCKS.get())) {
            filters.add(new BlockScanFilter(block));
        }

        // Extra rare block tags (beyond the implicit top-level-ore-tag rule)
        for (final TagKey<Block> tag : ConfigParsers.parseBlockTags(ModConfig.RARE_ORE_TAGS.get())) {
            filters.add(new BlockTagScanFilter(tag));
        }

        // Implicit rule: anything in the top-level ore tag that is neither
        // common nor ignored counts as rare.
        final TagKey<Block> topLevelOreTag = ConfigParsers.parseBlockTag(ModConfig.RARE_ORE_TOP_TAG.get());
        if (topLevelOreTag != null) {
            filters.add(state -> !IgnoredBlocks.contains(state)
                    && state.is(topLevelOreTag)
                    && !CommonOresBlockScannerModule.INSTANCE.getFilter(ItemStack.EMPTY).test(state));
        }

        if (filters.isEmpty()) {
            filter = state -> false;
        } else {
            filter = new BlockCacheScanFilter(filters);
        }
    }
}
