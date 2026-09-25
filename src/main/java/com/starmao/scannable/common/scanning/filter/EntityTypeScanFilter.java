package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.function.Predicate;

/** Matches a single entity type. */
public record EntityTypeScanFilter(EntityType<?> entityType) implements Predicate<Entity> {
    @Override
    public boolean test(Entity entity) {
        return Objects.equals(entityType, entity.getType());
    }
}
