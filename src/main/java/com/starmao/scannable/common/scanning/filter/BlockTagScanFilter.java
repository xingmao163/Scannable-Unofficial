package com.starmao.scannable.common.scanning.filter;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public record BlockTagScanFilter(TagKey<Block> tag) implements Predicate<BlockState> {
    @Override
    public boolean test(BlockState state) {
        return state.is(tag);
    }
}
