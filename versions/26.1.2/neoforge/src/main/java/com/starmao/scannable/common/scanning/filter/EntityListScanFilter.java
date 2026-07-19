package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Predicate;

/** Combines multiple entity predicates with OR logic. */
public record EntityListScanFilter(List<Predicate<Entity>> filters) implements Predicate<Entity> {
    @Override
    public boolean test(Entity entity) {
        return filters.stream().anyMatch(f -> f.test(entity));
    }
}
