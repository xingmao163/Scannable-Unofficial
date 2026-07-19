package com.starmao.scannable.common.scanning.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;

/**
 * A scan filter that pre-computes a set of matching blocks from multiple
 * predicates at construction time, then performs fast O(1) lookups during scanning.
 * <p>Iterates over the entire {@link BuiltInRegistries#BLOCK block registry} once,
 * testing each block against the supplied predicates, and caches the matching
 * blocks in a {@link HashSet}. This trades a one-time upfront cost for consistent
 * per-block performance during active scanning.
 */
public final class BlockCacheScanFilter implements Predicate<BlockState> {
    private final Collection<Block> blocks;

    /**
     * Constructs a cache from a collection of block state predicates.
     *
     * @param filters the predicates to test each registered block against
     */
    public BlockCacheScanFilter(Collection<Predicate<BlockState>> filters) {
        blocks = buildCache(filters);
    }

    /**
     * Constructs a filter that accepts exactly the given blocks.
     *
     * @param blocks the blocks to detect
     */
    public BlockCacheScanFilter(List<Block> blocks) {
        this.blocks = new HashSet<>(blocks);
    }
}
