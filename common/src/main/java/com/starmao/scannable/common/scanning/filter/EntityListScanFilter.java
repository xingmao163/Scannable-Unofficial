package com.starmao.scannable.common.scanning.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Matches entities from a list of registry names. */
public final class EntityListScanFilter implements Predicate<Entity> {
    private final List<EntityType<?>> types = new ArrayList<>();

    public EntityListScanFilter(List<ResourceLocation> locations) {
        for (ResourceLocation loc : locations) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(loc).ifPresent(types::add);
        }
    }

    @Override
    public boolean test(Entity entity) {
        return types.contains(entity.getType());
    }
}
