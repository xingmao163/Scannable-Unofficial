package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.FriendlyEntityScanFilter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Scanner module that detects friendly / passive entities (animals, villagers, etc.).
 * <p>Delegates filtering to {@link FriendlyEntityScanFilter#INSTANCE}.
 * Singleton enum — stateless.
 */
public enum FriendlyEntityScannerModule implements EntityScannerModule {
    INSTANCE;

    /**
     * {@return the energy cost in FE per scan tick, from the friendly entity scanner config}
     */
    @Override
    public int getEnergyCost(ItemStack module) {
        return ModConfig.SCANNER_ENERGY_COST_FRIENDLY.get();
    }

    /**
     * {@return the result provider that displays entity scan results}
     */
    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("entities");
    }

    /**
     * {@return the filter predicate that matches friendly entities}
     */
    @Override
    public Predicate<Entity> getFilter(ItemStack module) {
        return FriendlyEntityScanFilter.INSTANCE;
    }
}
