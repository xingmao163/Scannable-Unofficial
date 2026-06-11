package com.starmao.scannable.network;

import net.minecraft.core.BlockPos;

public record ScanResultEntry(BlockPos pos, String displayName, int remainingBlocks, String oreType) {
}
