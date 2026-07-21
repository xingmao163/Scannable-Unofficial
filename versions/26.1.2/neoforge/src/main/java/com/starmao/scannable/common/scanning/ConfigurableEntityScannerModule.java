package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.common.scanning.filter.EntityListScanFilter;
import com.starmao.scannable.common.scanning.filter.EntityTypeScanFilter;
import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Scanner module that detects specific entity types configured by the player.
 *
 * <p>Reads the target entity list from the module item's data component
 * via {@link ConfigurableEntityScannerModuleItem}, resolving registry names
 * to entity types at scan time via {@link EntityListScanFilter}.
 *
 * <p>Singleton enum — stateless beyond config lookups.
 */
public enum ConfigurableEntityScannerModule implements EntityScannerModule {
    INSTANCE;

    /**
     * {@return the energy cost in FE per scan tick, from the entity scanner config}
     */
    @Override
    public int getEnergyCost(ItemStack module) {
        return ServerConfig.SCANNER_ENERGY_COST_ENTITY.get();
    }

    /**
     * {@return the result provider that displays entity scan results}
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviderRegistry.get("entities");
    }

    /**
     * Builds a filter predicate from the configured entity type IDs on the module item.
     *
     * @param module the scanner module item stack containing configured entity type IDs
     * @return a predicate that tests entities against the configured type list
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public Predicate<Entity> getFilter(ItemStack module) {
        List<Identifier> ids = List.of();
        if (module.getItem() instanceof ConfigurableEntityScannerModuleItem item) {
            ids = item.getIds(module);
        }
        final List<Predicate<Entity>> filters = ids.stream()
                .map(id -> BuiltInRegistries.ENTITY_TYPE.getOptional(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(EntityTypeScanFilter::new)
                .collect(Collectors.toList());
        return new EntityListScanFilter(filters);
    }
}
