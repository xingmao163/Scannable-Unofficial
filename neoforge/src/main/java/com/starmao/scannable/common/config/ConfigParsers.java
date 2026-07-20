package com.starmao.scannable.common.config;

import com.starmao.scannable.Scannable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reusable parsers for {@link ModConfigSpec} {@code List<String>} values that
 * represent registry names, tags, or block/fluid IDs.
 *
 * <p>ModConfigSpec flattens complex types ({@code Set<ResourceLocation>},
 * {@code Set<TagKey>}) into {@code List<String>}. These methods centralise
 * the {@code tryParse → lookup → warn} pattern so callers don't repeat it.
 */
public final class ConfigParsers {

    /**
     * Parse a string list into {@link Block} references, skipping invalid entries
     * with a warning.
     */
    public static List<Block> parseBlocks(final List<? extends String> entries) {
        final List<Block> blocks = new ArrayList<>();
        for (final String entry : entries) {
            final ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc == null) {
                Scannable.LOGGER.warn("Invalid block registry name in config: {}", entry);
                continue;
            }
            BuiltInRegistries.BLOCK.getOptional(loc).ifPresentOrElse(blocks::add,
                    () -> Scannable.LOGGER.warn("Unknown block in config: {}", entry));
        }
        return blocks;
    }

    /**
     * Parse a string list into {@link TagKey TagKeys} for the block registry,
     * skipping invalid entries with a warning.
     */
    public static Set<TagKey<Block>> parseBlockTags(final List<? extends String> entries) {
        final Set<TagKey<Block>> tags = new HashSet<>();
        for (final String entry : entries) {
            final ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc == null) {
                Scannable.LOGGER.warn("Invalid block tag name in config: {}", entry);
                continue;
            }
            tags.add(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, loc));
        }
        return tags;
    }

    /**
     * Parse a string list into {@link TagKey TagKeys} for the fluid registry,
     * skipping invalid entries with a warning.
     */
    public static Set<TagKey<Fluid>> parseFluidTags(final List<? extends String> entries) {
        final Set<TagKey<Fluid>> tags = new HashSet<>();
        for (final String entry : entries) {
            final ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc == null) {
                Scannable.LOGGER.warn("Invalid fluid tag name in config: {}", entry);
                continue;
            }
            tags.add(TagKey.create(net.minecraft.core.registries.Registries.FLUID, loc));
        }
        return tags;
    }

    /**
     * Parse a single string config entry into a {@link TagKey} for the block registry.
     * Returns {@code null} if the entry is blank or invalid.
     */
    public static TagKey<Block> parseBlockTag(final String entry) {
        if (entry == null || entry.isBlank()) return null;
        final ResourceLocation loc = ResourceLocation.tryParse(entry);
        if (loc == null) {
            Scannable.LOGGER.warn("Invalid block tag in config: {}", entry);
            return null;
        }
        return TagKey.create(net.minecraft.core.registries.Registries.BLOCK, loc);
    }

    /**
     * Iterate over a string list, calling {@code consumer} for each entry that
     * parses as a valid {@link ResourceLocation}.
     */
    public static void forEachLocation(final List<? extends String> entries,
                                       final Consumer<ResourceLocation> consumer) {
        for (final String entry : entries) {
            final ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc == null) {
                Scannable.LOGGER.warn("Invalid resource location in config: {}", entry);
                continue;
            }
            consumer.accept(loc);
        }
    }

    private ConfigParsers() {}
}
