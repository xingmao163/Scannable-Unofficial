package com.starmao.scannable.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Central common configuration for the scanner mod.
 * <p>Defines all server-authoritative config values using NeoForge's
 * {@link ModConfigSpec} system, covering energy costs, scan ranges,
 * ignored blocks/fluids, and debug flags.
 * <p>Config file: {@code config/scannable_unofficial-server.toml}
 */
public final class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- Debug ----

    /** Debug logging for item scanner operations. */
    public static final ModConfigSpec.BooleanValue DEBUG_LOG_ITEM_SCANNER = BUILDER
            .comment("Enable debug logging for item scanner operations (scanning process and statistics).")
            .define("debug.logItemScanner", false);

    // ---- Scanner ----

    /** Whether the scanner consumes energy when performing a scan. */
    public static final ModConfigSpec.BooleanValue SCANNER_USE_ENERGY = BUILDER
            .comment("Whether the scanner consumes energy when performing a scan.")
            .define("scanner.useEnergy", true);

    /** Maximum FE energy capacity of the scanner item. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_CAPACITY = BUILDER
            .comment("Amount of energy that can be stored in a scanner.")
            .defineInRange("scanner.energyCapacity", 20000, 1, Integer.MAX_VALUE);

    /** Base scan radius in blocks (without range modules). */
    public static final ModConfigSpec.IntValue SCANNER_BASE_RADIUS = BUILDER
            .comment("The basic scan radius without range modules. Higher values increase computational overhead.")
            .defineInRange("scanner.baseScanRadius", 64, 16, 128);

    /** Duration (ms) that scan results remain visible on screen. */
    public static final ModConfigSpec.IntValue SCANNER_RESULT_STAY_DURATION = BUILDER
            .comment("How long scan results remain visible, in milliseconds.")
            .defineInRange("scanner.resultStayDuration", 10000, 1000, 60000 * 5);

    // ---- Energy Costs ----

    /** Energy cost per scan for the range module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_RANGE = BUILDER
            .comment("Energy cost of the range module per scan.")
            .defineInRange("energy.range", 100, 0, 10000);

    /** Energy cost per scan for the fluid module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_FLUID = BUILDER
            .comment("Energy cost of the fluid module per scan.")
            .defineInRange("energy.fluid", 50, 0, 10000);

    /** Energy cost per scan for the friendly entity module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_FRIENDLY = BUILDER
            .comment("Energy cost of the friendly entity module per scan.")
            .defineInRange("energy.friendly", 25, 0, 10000);

    /** Energy cost per scan for the hostile entity module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_HOSTILE = BUILDER
            .comment("Energy cost of the hostile entity module per scan.")
            .defineInRange("energy.hostile", 50, 0, 10000);

    /** Energy cost per scan for the configurable block module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_BLOCK = BUILDER
            .comment("Energy cost of the configurable block module per scan.")
            .defineInRange("energy.block", 100, 0, 10000);

    /** Energy cost per scan for the configurable entity module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_ENTITY = BUILDER
            .comment("Energy cost of the configurable entity module per scan.")
            .defineInRange("energy.entity", 75, 0, 10000);

    /** Energy cost per scan for the item scanner module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_ITEM = BUILDER
            .comment("Energy cost of the item scanner module per scan.")
            .defineInRange("energy.item", 100, 0, 10000);

    // ---- Range Modifiers ----

    /** Relative scan radius increase per installed range module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_RANGE = BUILDER
            .comment("Relative scan radius added by each range module.")
            .defineInRange("range.range", 0.5, 0.0, 1.0);

    /** Relative effective scan range multiplier for the fluid module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_FLUID = BUILDER
            .comment("Relative effective range of the fluid module.")
            .defineInRange("range.fluid", 0.5, 0.0, 1.0);

    /** Relative effective scan range multiplier for the block module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_BLOCK = BUILDER
            .comment("Relative effective range of the configurable block module.")
            .defineInRange("range.block", 0.5, 0.0, 1.0);

    // ---- Fluids ----

    /** Fluid tags that the fluid scanner module should skip. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_FLUID_TAGS = BUILDER
            .comment("Fluid tags of fluids that should be ignored by the fluid scanner module.")
            .defineListAllowEmpty(List.of("fluids.ignoredTags"),
                    List::of,
                    entry -> entry instanceof String);

    // ---- Ignored Blocks ----

    /** Registry names of blocks ignored by all scanner modules. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_BLOCKS = BUILDER
            .comment("Registry names of blocks that should be ignored by all scanner modules.")
            .defineListAllowEmpty(List.of("ignored.blocks"),
                    () -> List.of("minecraft:command_block"),
                    entry -> entry instanceof String);

    /** Block tags whose members are ignored by all scanner modules. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_BLOCK_TAGS = BUILDER
            .comment("Block tag names of tags that should be ignored by all scanner modules.")
            .defineListAllowEmpty(List.of("ignored.blockTags"),
                    List::of,
                    entry -> entry instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModConfig() {}
}
