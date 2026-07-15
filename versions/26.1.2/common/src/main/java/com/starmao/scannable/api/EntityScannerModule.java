package com.starmao.scannable.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Specialization of ScannerModule for the built-in entity scan result provider.
 * In NeoForge 26.1.2+, ResourceLocation has been moved; uses Object as return type.
 */
public interface EntityScannerModule extends ScannerModule {
    default Optional<Object> getIcon(Entity entity) {
        return Optional.empty();
    }

    Predicate<Entity> getFilter(ItemStack module);
}
