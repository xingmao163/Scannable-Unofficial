package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScannerModule;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public enum RangeScannerModule implements ScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_RANGE.get();
    }

    @Override
    public boolean hasResultProvider() {
        return false;
    }

    @Nullable
    @Override
    public ScanResultProvider getResultProvider() {
        return null;
    }

    @Override
    public float adjustGlobalRange(float range) {
        return range + Mth.ceil(ModConfig.SCANNER_BASE_RADIUS.get() * ModConfig.SCANNER_RANGE_MODIFIER_RANGE.get());
    }
}
