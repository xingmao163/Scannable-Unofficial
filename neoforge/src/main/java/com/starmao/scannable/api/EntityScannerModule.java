package com.starmao.scannable.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Specialization of ScannerModule for the built-in entity scan result provider.
 */
public interface EntityScannerModule extends ScannerModule {

    /**
     * Returns an optional icon to display for the given entity in scan results.
     *
     * @param entity the detected entity
     * @return an {@link Optional} containing a {@link ResourceLocation} texture path,
     *         or empty to use the default icon
     */
    default Optional<ResourceLocation> getIcon(Entity entity) {
        return Optional.empty();
    }

    /**
     * Returns a predicate that filters which entities this module detects.
     *
     * @param module the specific item stack instance of this module
     * @return a predicate that returns {@code true} for detectable entities
     */
    Predicate<Entity> getFilter(ItemStack module);
}
