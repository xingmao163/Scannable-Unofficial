package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * A scan filter that matches a single specific block by identity comparison.
 * <p>Used when a player configures a {@link com.starmao.scannable.api.BlockScannerModule}
 * to detect a particular block type.
 *
 * @param block the block to detect
 */
public record BlockScanFilter(Block block) implements Predicate<BlockState> {
