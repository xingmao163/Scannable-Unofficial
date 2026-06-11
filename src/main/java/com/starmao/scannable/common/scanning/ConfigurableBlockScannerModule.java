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

public enum ConfigurableBlockScannerModule implements BlockScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_BLOCK.get();
    }

    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("blocks");
    }

    @Override
    public float adjustLocalRange(float range) {
        return range * (float) (double) ModConfig.SCANNER_RANGE_MODIFIER_BLOCK.get();
    }

    @Override
    public Predicate<BlockState> getFilter(ItemStack module) {
        List<Block> blocks = List.of();
        if (module.getItem() instanceof ConfigurableBlockScannerModuleItem item) {
            blocks = item.getValues(module);
        }
        return new BlockCacheScanFilter(blocks);
    }
}
