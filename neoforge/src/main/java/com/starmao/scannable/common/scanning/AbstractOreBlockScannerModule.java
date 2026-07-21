package com.starmao.scannable.common.scanning;

import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.config.ConfigParsers;
import com.starmao.scannable.common.scanning.filter.BlockCacheScanFilter;
import com.starmao.scannable.common.scanning.filter.BlockScanFilter;
import com.starmao.scannable.common.scanning.filter.BlockTagScanFilter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base implementation for ore-block scanner modules.
 *
 * <p>Provides lazy-built, cached block filters driven by configuration entries.
 * Subclasses supply the specific config keys via abstract methods and may
 * contribute extra filters via {@link #addExtraFilters}.
 *
 * <p>Singleton — use the {@code INSTANCE} constant of each subclass.
 */
public abstract class AbstractOreBlockScannerModule implements BlockScannerModule {

    private Predicate<BlockState> filter;

    /** Energy cost for this ore module. */
    protected abstract int getEnergyCostConfig();

    /** Range modifier for this ore module. */
    protected abstract float getRangeModifierConfig();

    /** Config list of specific block IDs. */
    protected abstract List<? extends String> getBlockConfig();

    /** Config list of block tag names. */
    protected abstract List<? extends String> getTagConfig();

    /**
     * Hook for subclasses to contribute extra filters beyond block-ids and tags.
     * Called during {@link #validateFilter()} after the standard filters are added.
     */
    protected void addExtraFilters(final List<Predicate<BlockState>> filters) {
        // No-op by default
    }

    /** Clears the cached filter so it is rebuilt on the next scan. */
    protected void clearFilter() {
        this.filter = null;
    }

    @Override
    public int getEnergyCost(final ItemStack module) {
        return getEnergyCostConfig();
    }

    @Override
    public float adjustLocalRange(final float range) {
        return range * getRangeModifierConfig();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get(ScanResultProviderRegistry.BLOCKS);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Predicate<BlockState> getFilter(final ItemStack module) {
        validateFilter();
        return filter;
    }

    @OnlyIn(Dist.CLIENT)
    private void validateFilter() {
        if (filter != null) return;

        final List<Predicate<BlockState>> filters = new ArrayList<>();

        for (final Block block : ConfigParsers.parseBlocks(getBlockConfig())) {
            filters.add(new BlockScanFilter(block));
        }
        for (final TagKey<Block> tag : ConfigParsers.parseBlockTags(getTagConfig())) {
            filters.add(new BlockTagScanFilter(tag));
        }

        addExtraFilters(filters);

        if (filters.isEmpty()) {
            filter = state -> false;
        } else {
            filter = new BlockCacheScanFilter(filters);
        }
    }
}
