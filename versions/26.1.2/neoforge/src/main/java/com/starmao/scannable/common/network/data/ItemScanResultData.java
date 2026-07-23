package com.starmao.scannable.common.network.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Network-serializable scan result data for the item scanner module.
 *
 * <p>Transferred server→client after an item scan completes. Contains the
 * position of the matching container, which item was found, and how many
 * of that item were detected across all slots.
 *
 * @param pos        Block position of the container that matched
 * @param itemId     Registry name of the matched item (e.g. {@code minecraft:diamond})
 * @param totalCount Total count of the matched item across all slots
 */
public record ItemScanResultData(BlockPos pos, Identifier itemId, int totalCount) {
}
