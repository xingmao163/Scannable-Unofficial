package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.BlockCacheScanFilter;
import com.starmao.scannable.common.scanning.filter.FluidTagScanFilter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public enum FluidBlockScannerModule implements BlockScannerModule {
    INSTANCE;

    private Predicate<BlockState> filter;

    public static void clearCache() {
        INSTANCE.filter = null;
    }

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_FLUID.get();
    }

    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("blocks");
    }

    @Override
    public float adjustLocalRange(float range) {
        return range * (float) (double) ModConfig.SCANNER_RANGE_MODIFIER_FLUID.get();
    }

    @Override
    public Predicate<BlockState> getFilter(ItemStack module) {
        validateFilter();
        return filter;
    }

    private void validateFilter() {
        if (filter != null) return;

        List<Predicate<BlockState>> filters = new ArrayList<>();
        List<? extends String> ignoredTags = ModConfig.IGNORED_FLUID_TAGS.get();
        BuiltInRegistries.FLUID.getTags().forEach(entry -> {
            var tag = entry.key();
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
