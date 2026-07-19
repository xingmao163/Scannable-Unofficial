package com.starmao.scannable.common.scanning.filter;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * A scan filter that matches any block belonging to a given block tag.
 * <p>Used to detect groups of blocks (e.g. {@code c:ores/*}) without
 * enumerating every individual block.
 *
 * @param tag the block tag to match against
 */
public record BlockTagScanFilter(TagKey<Block> tag) implements Predicate<BlockState> {
