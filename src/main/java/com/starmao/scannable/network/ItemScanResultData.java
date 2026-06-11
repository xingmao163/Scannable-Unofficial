package com.starmao.scannable.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Network-serializable scan result data for item scanner.
 * Transferred from server to client — no client-only classes involved.
 *
 * @param pos       Block position of the matching container
 * @param itemId    Registry name of the matched item (e.g. "minecraft:diamond")
 * @param totalCount Total count of the matched item across all slots
 */
public record ItemScanResultData(BlockPos pos, ResourceLocation itemId, int totalCount) {
}
