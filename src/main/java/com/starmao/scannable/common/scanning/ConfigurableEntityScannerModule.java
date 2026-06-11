package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.EntityListScanFilter;
import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public enum ConfigurableEntityScannerModule implements EntityScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_ENTITY.get();
    }

    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("entities");
    }

    @Override
    public Predicate<Entity> getFilter(ItemStack module) {
        List<ResourceLocation> ids = List.of();
        if (module.getItem() instanceof ConfigurableEntityScannerModuleItem item) {
            ids = item.getIds(module);
        }
        return new EntityListScanFilter(ids);
    }
}
