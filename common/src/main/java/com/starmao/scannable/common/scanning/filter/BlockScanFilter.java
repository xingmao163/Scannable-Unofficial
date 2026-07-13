package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public record BlockScanFilter(Block block) implements Predicate<BlockState> {
    @Override
    public boolean test(BlockState state) {
        return block == state.getBlock();
    }
}
