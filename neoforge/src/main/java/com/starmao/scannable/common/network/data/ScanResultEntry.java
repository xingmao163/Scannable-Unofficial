package com.starmao.scannable.common.network.data;

import net.minecraft.core.BlockPos;

/**
 * A single scan result entry displayed in the scanner overlay.
 *
 * @param pos            World position of the detected object
 * @param displayName    Human-readable name (e.g. "Iron Ore")
 * @param remainingCount Number of remaining blocks at this position (for clustered results)
 * @param hint           Contextual hint (e.g. ore type classification)
 */
public record ScanResultEntry(BlockPos pos, String displayName, int remainingCount, String hint) {
}
