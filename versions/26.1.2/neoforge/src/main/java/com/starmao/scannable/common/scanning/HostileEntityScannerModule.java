package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.HostileEntityScanFilter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Scanner module that detects hostile / enemy entities (monsters).
 * <p>Delegates filtering to {@link HostileEntityScanFilter#INSTANCE}
 * and displays a warning icon ({@link ModTextures#ICON_WARNING}) on results.
 * Singleton enum — stateless.
 */
public enum HostileEntityScannerModule implements EntityScannerModule {
    INSTANCE;

    /**
     * {@return the energy cost in FE per scan tick, from the hostile entity scanner config}
     */
    @Override
    public int getEnergyCost(ItemStack module) {
        return ServerConfig.SCANNER_ENERGY_COST_HOSTILE.get();
    }

    /**
     * {@return the result provider that displays entity scan results}
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("entities");
    }

    /**
     * {@return the warning icon overlay for hostile entity scan results}
     */
    @Override
    @SuppressWarnings({"rawtypes"})
    public Optional getIcon(Entity entity) {
        return Optional.of(com.starmao.scannable.Scannable.id("textures/gui/overlay/warning.png"));
    }

    /**
     * {@return the filter predicate that matches hostile entities}
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public Predicate<Entity> getFilter(ItemStack module) {
        return HostileEntityScanFilter.INSTANCE;
    }
}
