package com.starmao.scannable.common.scanning.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;

/** Pre-computes a set of matching blocks from predicates. */
public final class BlockCacheScanFilter implements Predicate<BlockState> {
    private final Collection<Block> blocks;

    public BlockCacheScanFilter(Collection<Predicate<BlockState>> filters) {
        blocks = buildCache(filters);
    }

    public BlockCacheScanFilter(List<Block> blocks) {
        this.blocks = new HashSet<>(blocks);
    }

    @Override
    public boolean test(BlockState state) {
        return blocks.contains(state.getBlock());
    }

    private static Collection<Block> buildCache(Collection<Predicate<BlockState>> filters) {
        Set<Block> cache = new HashSet<>();
        BuiltInRegistries.BLOCK.forEach(block -> {
            BlockState blockState = block.defaultBlockState();
            if (filters.stream().anyMatch(f -> f.test(blockState))) {
                cache.add(blockState.getBlock());
            }
        });
        return cache;
    }
}
