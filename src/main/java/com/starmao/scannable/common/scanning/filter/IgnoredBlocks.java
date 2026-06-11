package com.starmao.scannable.common.scanning.filter;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Maintains a set of blocks and block tags that should be ignored by all scanner
 * modules. Config changes are picked up when {@link #clearCache()} is called.
 */
public final class IgnoredBlocks {
    private static Set<Block> ignoredBlocks;
    private static Set<TagKey<Block>> ignoredBlockTags;

    public static boolean contains(final BlockState state) {
        validate();
        if (ignoredBlocks.contains(state.getBlock())) return true;
        for (final TagKey<Block> tag : ignoredBlockTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    public static void clearCache() {
        ignoredBlocks = null;
        ignoredBlockTags = null;
    }

    private static void validate() {
        if (ignoredBlocks != null && ignoredBlockTags != null) return;

        ignoredBlocks = new HashSet<>();
        ignoredBlockTags = new HashSet<>();

        for (final String entry : ModConfig.IGNORED_BLOCKS.get()) {
            ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc != null) {
                BuiltInRegistries.BLOCK.getOptional(loc).ifPresent(ignoredBlocks::add);
            } else {
                Scannable.LOGGER.warn("Invalid ignored block entry: {}", entry);
            }
        }

        for (final String entry : ModConfig.IGNORED_BLOCK_TAGS.get()) {
            ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc != null) {
                ignoredBlockTags.add(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, loc));
            } else {
                Scannable.LOGGER.warn("Invalid ignored block tag entry: {}", entry);
            }
        }
    }

    private IgnoredBlocks() {}
}
