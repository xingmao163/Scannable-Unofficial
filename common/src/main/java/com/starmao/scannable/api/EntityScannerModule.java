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
    default Optional<ResourceLocation> getIcon(Entity entity) {
        return Optional.empty();
    }

    Predicate<Entity> getFilter(ItemStack module);
}
