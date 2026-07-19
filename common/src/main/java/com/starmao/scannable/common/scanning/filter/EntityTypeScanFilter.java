package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.function.Predicate;

/** Matches entities by type list. */
public record EntityTypeScanFilter(List<EntityType<?>> types) implements Predicate<Entity> {
    @Override
    public boolean test(Entity entity) {
        return types.contains(entity.getType());
    }
}
