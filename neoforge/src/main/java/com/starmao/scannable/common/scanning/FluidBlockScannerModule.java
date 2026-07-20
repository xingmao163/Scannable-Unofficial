package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.BlockCacheScanFilter;
import com.starmao.scannable.common.scanning.filter.FluidTagScanFilter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Scanner module that detects fluid blocks (water, lava, etc.).
 *
 * <p>Iterates over all registered fluid tags, builds a filter for each
 * non-ignored tag, and caches the matching blocks in a
 * {@link BlockCacheScanFilter}. The cache is invalidated by
 * {@link #clearCache()} when the ignored-tag config changes.
 *
 * <p>Singleton enum with a lazily-built, invalidable filter cache.
 */
public enum FluidBlockScannerModule implements BlockScannerModule {
    INSTANCE;

    private Predicate<BlockState> filter;

    /**
     * Clears the cached fluid filter so it is rebuilt on the next scan.
     * Should be called when fluid tag config changes are applied.
     */
    public static void clearCache() {
        INSTANCE.filter = null;
    }

    /**
     * {@return the energy cost in FE per scan tick, from the fluid scanner config}
     */
    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_FLUID.get();
    }

    /**
     * {@return the result provider that displays block scan results for fluid blocks}
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get(ScanResultProviderRegistry.BLOCKS);
    }

    /**
     * Applies the scanner's range modifier to the local scan radius for fluid detection.
     *
     * @param range the base range value
     * @return the adjusted range after applying the fluid range modifier
     */
    @Override
    public float adjustLocalRange(float range) {
        return range * (float) (double) ModConfig.SCANNER_RANGE_MODIFIER_FLUID.get();
    }

    /**
     * {@return the cached fluid block filter, rebuilding it if necessary}
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public Predicate<BlockState> getFilter(ItemStack module) {
        validateFilter();
        return filter;
    }

    private void validateFilter() {
        if (filter != null) return;

        List<Predicate<BlockState>> filters = new ArrayList<>();
        List<? extends String> ignoredTags = ModConfig.IGNORED_FLUID_TAGS.get();
        BuiltInRegistries.FLUID.getTagNames().forEach(tag -> {
            if (!ignoredTags.contains(tag.location().toString())) {
                filters.add(new FluidTagScanFilter(tag));
            }
        });
        if (filters.isEmpty()) {
            // Fallback: if all tags ignored, match nothing so scanner does nothing
            filters.add(state -> false);
        }
        filter = new BlockCacheScanFilter(filters);
    }
}
