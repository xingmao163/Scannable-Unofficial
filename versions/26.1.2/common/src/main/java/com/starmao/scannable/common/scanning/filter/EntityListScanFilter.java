package com.starmao.scannable.common.scanning.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Entity type list-based scan filter.
 * In NeoForge 26.1.2+, uses reflection to handle ResourceLocation without direct import.
 */
public final class EntityListScanFilter implements Predicate<Entity> {
    private final List<EntityType<?>> types = new ArrayList<>();

    public EntityListScanFilter(List<Object> locations) {
        for (Object loc : locations) {
            if (loc != null && isResourceLocation(loc)) {
                try {
                    // Use reflection to call registry lookup safely
                    Optional<?> optional = (Optional<?>) lookupEntityType(loc);
                    optional.ifPresent(obj -> {
                        if (obj instanceof EntityType<?>) {
                            types.add((EntityType<?>) obj);
                        }
                    });
                } catch (Exception e) {
                    // Silently ignore invalid locations
                }
            }
        }
    }

    private static boolean isResourceLocation(Object obj) {
        return obj != null && obj.getClass().getName().equals("net.minecraft.resources.ResourceLocation");
    }

    private static Object lookupEntityType(Object resourceLocation) throws Exception {
        // Use reflection to safely invoke registry lookup
        var getOptionalMethod = BuiltInRegistries.ENTITY_TYPE.getClass().getMethod("getOptional", Object.class);
        return getOptionalMethod.invoke(BuiltInRegistries.ENTITY_TYPE, resourceLocation);
    }

    @Override
    public boolean test(Entity entity) {
        return types.contains(entity.getType());
    }
}
