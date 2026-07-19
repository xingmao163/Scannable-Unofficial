package com.starmao.scannable.common.scanning.filter;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

/**
 * A scan filter that matches block states containing a fluid from a given fluid tag.
 * <p>Used by the fluid scanner module to detect fluids like water and lava
 * by their tags (e.g. {@code #minecraft:water}).
 *
 * @param tag the fluid tag to match against
 */
public record FluidTagScanFilter(TagKey<Fluid> tag) implements Predicate<BlockState> {
