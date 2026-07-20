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

    /** When true, the scanner can only be charged by the charger module, not by external FE sources. */
    public static final ModConfigSpec.BooleanValue SCANNER_CHARGE_ONLY_BY_MODULE = BUILDER
            .comment("When true, the scanner can only be charged by the charger module, not by external FE sources like chargers from other mods.")
            .define("scanner.chargeOnlyByModule", false);

    /** Maximum FE energy capacity of the scanner item. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_CAPACITY = BUILDER
            .comment("Amount of energy that can be stored in a scanner.")
            .defineInRange("scanner.energyCapacity", 5000, 1, Integer.MAX_VALUE);

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
            .defineInRange("energy.rangeEnergy", 100, 0, 10000);

    /** Energy cost per scan for the fluid module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_FLUID = BUILDER
            .comment("Energy cost of the fluid module per scan.")
            .defineInRange("energy.fluidEnergy", 50, 0, 10000);

    /** Energy cost per scan for the friendly entity module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_FRIENDLY = BUILDER
            .comment("Energy cost of the friendly entity module per scan.")
            .defineInRange("energy.friendlyEnergy", 25, 0, 10000);

    /** Energy cost per scan for the hostile entity module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_HOSTILE = BUILDER
            .comment("Energy cost of the hostile entity module per scan.")
            .defineInRange("energy.hostileEnergy", 50, 0, 10000);

    /** Energy cost per scan for the configurable block module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_BLOCK = BUILDER
            .comment("Energy cost of the configurable block module per scan.")
            .defineInRange("energy.blockEnergy", 100, 0, 10000);

    /** Energy cost per scan for the configurable entity module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_ENTITY = BUILDER
            .comment("Energy cost of the configurable entity module per scan.")
            .defineInRange("energy.entityEnergy", 75, 0, 10000);

    /** Energy cost per scan for the item scanner module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_ITEM = BUILDER
            .comment("Energy cost of the item scanner module per scan.")
            .defineInRange("energy.itemEnergy", 100, 0, 10000);

    /** Energy cost per scan for the common ores module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_ORE_COMMON = BUILDER
            .comment("Energy cost of the common ores module per scan.")
            .defineInRange("energy.commonOreEnergy", 75, 0, 10000);

    /** Energy cost per scan for the rare ores module. */
    public static final ModConfigSpec.IntValue SCANNER_ENERGY_COST_ORE_RARE = BUILDER
            .comment("Energy cost of the rare ores module per scan.")
            .defineInRange("energy.rareOreEnergy", 100, 0, 10000);


    // ---- Charger Module ----

    /** Ticks between each charge pulse from the charger module. */
    public static final ModConfigSpec.IntValue CHARGER_MODULE_INTERVAL = BUILDER
            .comment("Ticks between each charge pulse from the charger module.")
            .defineInRange("charger.intervalTicks", 100, 1, 72000);

    /** FE added per charge pulse, per installed charger module. */
    public static final ModConfigSpec.IntValue CHARGER_MODULE_ENERGY_PER_PULSE = BUILDER
            .comment("FE added per charge pulse, per installed charger module.")
            .defineInRange("charger.energyPerPulse", 10, 1, 10000);
    // ---- Range Modifiers ----

    /** Relative scan radius increase per installed range module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_RANGE = BUILDER
            .comment("Relative scan radius added by each range module.")
            .defineInRange("range.rangeRange", 0.5, 0.0, 1.0);

    /** Relative effective scan range multiplier for the common ores module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_ORE_COMMON = BUILDER
            .comment("Relative effective range of the common ores module.")
            .defineInRange("range.commonOreRange", 0.25, 0.0, 1.0);

    /** Relative effective scan range multiplier for the rare ores module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_ORE_RARE = BUILDER
            .comment("Relative effective range of the rare ores module.")
            .defineInRange("range.rareOreRange", 0.25, 0.0, 1.0);

    /** Relative effective scan range multiplier for the fluid module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_FLUID = BUILDER
            .comment("Relative effective range of the fluid module.")
            .defineInRange("range.fluidRange", 0.5, 0.0, 1.0);

    /** Relative effective scan range multiplier for the block module. */
    public static final ModConfigSpec.DoubleValue SCANNER_RANGE_MODIFIER_BLOCK = BUILDER
            .comment("Relative effective range of the configurable block module.")
            .defineInRange("range.blockRange", 0.5, 0.0, 1.0);

    // ---- Fluids ----

    /** Fluid tags that the fluid scanner module should skip. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_FLUID_TAGS = BUILDER
            .comment("Fluid tags of fluids that should be ignored by the fluid scanner module.")
            .defineListAllowEmpty(List.of("ignored.fluidsTags"),
                    List::of,
                    entry -> entry instanceof String);

    // ---- Ignored Blocks ----

    /** Registry names of blocks ignored by all scanner modules. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_BLOCKS = BUILDER
            .comment("Registry names of blocks that should be ignored by all scanner modules.")
            .defineListAllowEmpty(List.of("ignored.blocks"),
                    () -> List.of("minecraft:command_block"),
                    entry -> entry instanceof String);


    // ---- Common Ores ----

    /** Tag IDs for blocks to detect with the common ores module. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COMMON_ORE_TAGS = BUILDER
            .comment("Block tags whose members are detected by the common ores scanner module.",
                    "Default: common ore sub-tags (coal, iron, copper, gold, lapis, redstone).")
            .defineListAllowEmpty(List.of("ores.commonTags"),
                    () -> List.of(
                            "c:ores/coal",
                            "c:ores/iron",
                            "c:ores/copper",
                            "c:ores/gold",
                            "c:ores/lapis",
                            "c:ores/redstone"
                    ),
                    entry -> entry instanceof String);

    /** Registry names of specific blocks to detect with the common ores module. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COMMON_ORE_BLOCKS = BUILDER
            .comment("Registry names of specific blocks detected by the common ores scanner module.")
            .defineListAllowEmpty(List.of("ores.commonBlocks"),
                    List::of,
                    entry -> entry instanceof String);

    // ---- Rare Ores ----

    /** The top-level ore tag used by the rare ores module as the implicit "all ores" source. */
    public static final ModConfigSpec.ConfigValue<String> RARE_ORE_TOP_TAG = BUILDER
            .comment("Top-level ore tag (e.g. \"c:ores\"). Blocks matching this tag,",
                    "but NOT matching common ore tags/blocks or ignored blocks,",
                    "are automatically detected as rare ores. Set to empty to disable this implicit rule.")
            .define("ores.rareTopTag", "c:ores");

    /** Extra tag IDs for blocks detected by the rare ores module (beyond the implicit top-tag rule). */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RARE_ORE_TAGS = BUILDER
            .comment("Extra block tags detected by the rare ores scanner module.",
                    "Blocks matching these tags are detected even if they are not in the top-level ore tag.")
            .defineListAllowEmpty(List.of("ores.rareTags"),
                    () -> List.of(
                            "c:ores/diamond",
                            "c:ores/emerald",
                            "c:ores/quartz",
                            "c:ores/netherite_scrap"
                    ),
                    entry -> entry instanceof String);

    /** Registry names of specific blocks detected by the rare ores module (beyond the implicit rule). */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RARE_ORE_BLOCKS = BUILDER
            .comment("Registry names of specific blocks detected by the rare ores scanner module.",
                    "These blocks are detected even if they are not in the top-level ore tag.")
            .defineListAllowEmpty(List.of("ores.rareBlocks"),
                    List::of,
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
