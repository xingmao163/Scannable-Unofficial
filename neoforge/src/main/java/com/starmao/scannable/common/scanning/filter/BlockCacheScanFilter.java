package com.starmao.scannable.common.scanning.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;

/** Pre-computes a set of matching blocks from predicates. */
public final class BlockCacheScanFilter implements Predicate<BlockState> {
    private final Set<Block> blocks;

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

    private static Set<Block> buildCache(Collection<Predicate<BlockState>> filters) {
        Set<Block> cache = new HashSet<>();
        for (Predicate<BlockState> filter : filters) {
            // Fast path: resolve tag filter directly without registry scan
            if (filter instanceof BlockTagScanFilter tagFilter) {
                BuiltInRegistries.BLOCK.getTag(tagFilter.tag()).ifPresent(holders ->
                        holders.forEach(holder -> cache.add(holder.value())));
                continue;
            }
            // Fast path: single-block filter
            if (filter instanceof BlockScanFilter blockFilter) {
                cache.add(blockFilter.block());
                continue;
            }
            // Fallback: generic predicate requires scanning entire registry
            // This also covers all other predicates in the list
            return scanRegistry(filters);
        }
        return cache;
    }

    private static Set<Block> scanRegistry(Collection<Predicate<BlockState>> filters) {
        Set<Block> result = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            for (Predicate<BlockState> filter : filters) {
                if (filter.test(state)) {
                    result.add(block);
                    break;
                }
            }
        }
        return result;
    }
}
