package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.BlockCacheScanFilter;
import com.starmao.scannable.common.item.ConfigurableBlockScannerModuleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * Scanner module that detects specific blocks configured by the player.
 *
 * <p>Reads the target block list from the module item's data component
 * via {@link ConfigurableBlockScannerModuleItem}, then builds a
 * {@link BlockCacheScanFilter} for fast O(1) block matching at scan time.
 *
 * <p>Singleton enum — stateless beyond config lookups.
 */
public enum ConfigurableBlockScannerModule implements BlockScannerModule {
    INSTANCE;

    /**
     * {@return the energy cost in FE per scan tick, from the block scanner config}
     */
    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_BLOCK.get();
    }

    /**
     * {@return the result provider that displays block scan results}
     */
    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("blocks");
    }

    /**
     * Applies the scanner's range modifier to the local scan radius.
     *
     * @param range the base range value
     * @return the adjusted range after applying the block range modifier
     */
    @Override
    public float adjustLocalRange(float range) {
        return range * (float) (double) ModConfig.SCANNER_RANGE_MODIFIER_BLOCK.get();
    }

    /**
     * Builds a filter predicate from the configured block list on the module item.
     *
     * @param module the scanner module item stack containing configured blocks
     * @return a predicate that tests block states against the configured list
     */
    @Override
    public Predicate<BlockState> getFilter(ItemStack module) {
        List<Block> blocks = List.of();
        if (module.getItem() instanceof ConfigurableBlockScannerModuleItem item) {
            blocks = item.getValues(module);
        }
        return new BlockCacheScanFilter(blocks);
    }
}
