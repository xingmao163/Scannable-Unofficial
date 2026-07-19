package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.function.Predicate;

/**
 * A scan filter that matches entities whose type is contained in a pre-built list
 * of {@link EntityType entity types}.
 * <p>Unlike {@link EntityListScanFilter}, this operates directly on resolved
 * entity type references without registry lookups.
 *
 * @param types the entity types to detect
 */
public record EntityTypeScanFilter(List<EntityType<?>> types) implements Predicate<Entity> {
