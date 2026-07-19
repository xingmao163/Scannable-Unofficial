package com.starmao.scannable.client.config;

import com.starmao.scannable.Scannable;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side configuration for scan result rendering.
 *
 * <p>Config values are stored as {@code "key=0xRRGGBB"} strings in {@link ModConfigSpec.ConfigValue}
 * lists and lazily parsed into {@link Map Maps} on first access. The parsed maps are cached until
 * {@link #clearCache()} is called (triggered on config reload).
 *
 * <p>Generated config file: {@code config/scannable_unofficial-client.toml}
 */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========================================================================
    // Config definition
    // ========================================================================

    static { BUILDER.push("colors"); }

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_COLORS = BUILDER
            .comment("Colors for scanned blocks by registry name.",
                    "Each entry: \"namespace:path=0xRRGGBB\"",
                    "Example: \"minecraft:stone=0x808080\"")
            .defineListAllowEmpty(List.of("blocksColors"),
                    List::of,
                    entry -> entry instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_TAG_COLORS = BUILDER
            .comment("Colors for scanned blocks by block tag.",
                    "Each entry: \"namespace:path=0xRRGGBB\"",
                    "Example: \"c:ores/diamond=0x2EB1E0\"")
            .defineListAllowEmpty(List.of("blockTagsColors"),
                    ClientConfig::defaultBlockTagColors,
                    entry -> entry instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLUID_COLORS = BUILDER
            .comment("Colors for scanned fluids by fluid registry name.",
                    "Each entry: \"namespace:path=0xRRGGBB\"")
            .defineListAllowEmpty(List.of("fluidsColors"),
                    List::of,
                    entry -> entry instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLUID_TAG_COLORS = BUILDER
            .comment("Colors for scanned fluids by fluid tag.",
                    "Each entry: \"namespace:path=0xRRGGBB\"")
            .defineListAllowEmpty(List.of("fluidTagsColors"),
                    () -> List.of(
                            FluidTags.WATER.location() + "=0x" + Integer.toHexString(MapColor.WATER.col),
                            FluidTags.LAVA.location() + "=0x" + Integer.toHexString(MapColor.TERRACOTTA_ORANGE.col)
                    ),
                    entry -> entry instanceof String);

    static { BUILDER.pop(); }

    static { BUILDER.push("misc"); }

    public static final ModConfigSpec.BooleanValue HIDE_BROKEN_BLOCKS = BUILDER
            .comment("Hide the highlight of a scanned block once it has been broken or replaced,",
                    "without needing to rescan. Purely visual.")
            .define("hideBrokenBlocks", true);

    public static final ModConfigSpec.ConfigValue<String> ITEM_SCAN_COLOR = BUILDER
            .comment("Color for the item scanner module container highlights.",
                    "Format: \"0xRRGGBB\"",
                    "Example: \"0xBB44FF\" (purple)")
            .define("itemScanColor", "0xBB44FF");

    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // ========================================================================
    // Parsed color maps (lazily cached, cleared on config reload)
    // ========================================================================

    private static Map<ResourceLocation, Integer> blockColors;
    private static Map<ResourceLocation, Integer> blockTagColors;
    private static Map<ResourceLocation, Integer> fluidColors;
    private static Map<ResourceLocation, Integer> fluidTagColors;

    public static void clearCache() {
        blockColors = null;
        blockTagColors = null;
        fluidColors = null;
        fluidTagColors = null;
    }

    public static Map<ResourceLocation, Integer> getBlockColors() {
        if (blockColors == null) blockColors = parseColorList(BLOCK_COLORS.get());
        return blockColors;
    }

    public static Map<ResourceLocation, Integer> getBlockTagColors() {
        if (blockTagColors == null) blockTagColors = parseColorList(BLOCK_TAG_COLORS.get());
        return blockTagColors;
    }

    public static Map<ResourceLocation, Integer> getFluidColors() {
        if (fluidColors == null) fluidColors = parseColorList(FLUID_COLORS.get());
        return fluidColors;
    }

    public static Map<ResourceLocation, Integer> getFluidTagColors() {
        if (fluidTagColors == null) fluidTagColors = parseColorList(FLUID_TAG_COLORS.get());
        return fluidTagColors;
    }

    /**
     * Convenience: look up a block's configured color. Returns {@code null} if no override is set.
     */
    public static Integer getBlockColor(final Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        Integer color = getBlockColors().get(id);
        if (color != null) return color;
        for (var entry : getBlockTagColors().entrySet()) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, entry.getKey());
            if (block.defaultBlockState().is(tag)) return entry.getValue();
        }
        return null;
    }

    /**
     * Convenience: look up a fluid's configured color. Returns {@code null} if no override is set.
     */
    public static Integer getFluidColor(final Fluid fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        Integer color = getFluidColors().get(id);
        if (color != null) return color;
        for (var entry : getFluidTagColors().entrySet()) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, entry.getKey());
            if (fluid.defaultFluidState().is(tag)) return entry.getValue();
        }
        return null;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Parse a list of {@code "key=0xRRGGBB"} entries into an immutable color map.
     */
    private static Map<ResourceLocation, Integer> parseColorList(final List<? extends String> entries) {
        Map<ResourceLocation, Integer> result = new HashMap<>();
        for (String entry : entries) {
            if (entry == null) continue;
            int eq = entry.indexOf('=');
            if (eq < 1) {
                Scannable.LOGGER.warn("Skipping malformed color entry (no '=' found): {}", entry);
                continue;
            }
            String key = entry.substring(0, eq);
            String value = entry.substring(eq + 1);
            try {
                ResourceLocation id = ResourceLocation.parse(key);
                int color = value.startsWith("0x") || value.startsWith("0X")
                        ? Integer.parseUnsignedInt(value.substring(2), 16)
                        : Integer.parseUnsignedInt(value, 16);
                result.put(id, color);
            } catch (Exception e) {
                Scannable.LOGGER.warn("Invalid color config entry '{}': {}", entry, e.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    /**
     * Default block tag colors matching common ore block textures.
     */
    private static List<String> defaultBlockTagColors() {
        Object2IntMap<ResourceLocation> map = new Object2IntOpenHashMap<>();
        map.put(Tags.Blocks.ORES_COAL.location(), MapColor.COLOR_GRAY.col);
        map.put(Tags.Blocks.ORES_IRON.location(), MapColor.COLOR_BROWN.col);
        map.put(Tags.Blocks.ORES_GOLD.location(), MapColor.GOLD.col);
        map.put(Tags.Blocks.ORES_LAPIS.location(), MapColor.LAPIS.col);
        map.put(Tags.Blocks.ORES_DIAMOND.location(), MapColor.DIAMOND.col);
        map.put(Tags.Blocks.ORES_REDSTONE.location(), MapColor.COLOR_RED.col);
        map.put(Tags.Blocks.ORES_EMERALD.location(), MapColor.EMERALD.col);
        map.put(Tags.Blocks.ORES_QUARTZ.location(), MapColor.QUARTZ.col);
        map.put(ResourceLocation.parse("c:ores/tin"), MapColor.COLOR_CYAN.col);
        map.put(ResourceLocation.parse("c:ores/copper"), MapColor.TERRACOTTA_ORANGE.col);
        map.put(ResourceLocation.parse("c:ores/lead"), MapColor.TERRACOTTA_BLUE.col);
        map.put(ResourceLocation.parse("c:ores/silver"), MapColor.COLOR_LIGHT_GRAY.col);
        map.put(ResourceLocation.parse("c:ores/nickel"), MapColor.COLOR_LIGHT_BLUE.col);
        map.put(ResourceLocation.parse("c:ores/platinum"), MapColor.TERRACOTTA_WHITE.col);
        map.put(ResourceLocation.parse("c:ores/mithril"), MapColor.COLOR_PURPLE.col);

        return map.object2IntEntrySet().stream()
                .map(e -> e.getKey().toString() + "=0x" + Integer.toHexString(e.getIntValue()))
                .toList();
    }

    private ClientConfig() {}
}
