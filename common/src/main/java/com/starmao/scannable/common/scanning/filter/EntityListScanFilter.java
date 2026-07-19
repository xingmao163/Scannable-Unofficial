package com.starmao.scannable.common.scanning.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A scan filter that matches entities whose type is in a pre-resolved list.
 * <p>Resolves {@link ResourceLocation} registry names to {@link EntityType}
 * at construction time, so the actual scan loop performs fast list containment checks.
 *
 * @see com.starmao.scannable.api.EntityScannerModule
 */
public final class EntityListScanFilter implements Predicate<Entity> {
