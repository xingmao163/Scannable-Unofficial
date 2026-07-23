package com.starmao.scannable.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Specialization of ScannerModule for the built-in entity scan result provider.
 * <p>
 * <strong>Client-only filtering.</strong> The {@link #getFilter(ItemStack)} method
 * is called during scan result computation on the client side. While the method
 * signature uses only cross-platform types, it should only be implemented in
 * modules that run client-side.
 *
 * @see com.starmao.scannable.api.ScanResultProviderRegistry
 */
public interface EntityScannerModule extends ScannerModule {

    /**
     * Returns an optional icon to display for the given entity in scan results.
     * <p>
     * Only called if the entity was matched by the filter returned by
     * {@link #getFilter(ItemStack)}.
     * <p>
     * <strong>Client-side.</strong> The icon texture is used during scan result
     * rendering on the client. Override only in modules that run client-side.
     *
     * @param entity the detected entity
     * @return an {@link Optional} containing a {@link net.minecraft.resources.Identifier}
     *         texture path, or empty to use the default icon
     */
    default Optional<Identifier> getIcon(Entity entity) {
        return Optional.empty();
    }

    /**
     * Returns a predicate that filters which entities this module detects.
     * <p>
     * <strong>Client-only.</strong> Called during scan result computation on the
     * client. The predicate is evaluated against entities in the world to determine
     * which ones are highlighted.
     *
     * @param module the specific item stack instance of this module
     * @return a predicate that returns {@code true} for detectable entities
     */
    @OnlyIn(Dist.CLIENT)
    Predicate<Entity> getFilter(ItemStack module);
}
