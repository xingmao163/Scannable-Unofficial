package com.starmao.scannable.common.config;

/**
 * Central constants used across the scanner mod.
 * <p>This includes scan timing values and module configuration slots.
 * All values are compile-time constants.
 */
public final class Constants {
    /** Maximum number of configurable targets a configurable module can store (e.g. blocks, entities). */
    public static final int CONFIGURABLE_MODULE_SLOTS = 5;
    /** Duration of a single scan tick cycle in game ticks (40 ticks = 2 seconds). */
    public static final int SCAN_DURATION_TICKS = 40;
    /** Cooldown between consecutive scans in game ticks. */
    public static final int SCAN_COOLDOWN_TICKS = 40;

    private Constants() {
    }
}
