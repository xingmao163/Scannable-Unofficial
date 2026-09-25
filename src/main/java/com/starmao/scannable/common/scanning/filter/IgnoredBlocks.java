package com.starmao.scannable.common.scanning.filter;

import com.starmao.scannable.common.config.ConfigParsers;
import com.starmao.scannable.common.config.ServerConfig;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maintains a set of blocks and block tags that should be ignored by all scanner
 * modules. Config changes are picked up when {@link #clearCache()} is called.
 */
public final class IgnoredBlocks {
    private static Set<Block> ignoredBlocks;
    private static Set<TagKey<Block>> ignoredBlockTags;

    /**
     * Checks if a given block state matches any ignored block or block tag.
     */
    public static boolean contains(final BlockState state) {
        validate();
        if (ignoredBlocks.contains(state.getBlock())) return true;
        for (final TagKey<Block> tag : ignoredBlockTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    /**
     * Clears the cached ignored-block sets so they are rebuilt from config
     * on the next access. Called on config reload.
     */
    public static void clearCache() {
        ignoredBlocks = null;
        ignoredBlockTags = null;
    }

    private static void validate() {
        if (ignoredBlocks != null && ignoredBlockTags != null) return;

        final List<? extends String> blockEntries = ServerConfig.IGNORED_BLOCKS.get();
        ignoredBlocks = new HashSet<>(ConfigParsers.parseBlocks(blockEntries));

        final List<? extends String> tagEntries = ServerConfig.IGNORED_BLOCK_TAGS.get();
        ignoredBlockTags = ConfigParsers.parseBlockTags(tagEntries);
    }

    private IgnoredBlocks() {}
}
