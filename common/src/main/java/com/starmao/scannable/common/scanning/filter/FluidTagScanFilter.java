package com.starmao.scannable.common.scanning.filter;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

public record FluidTagScanFilter(TagKey<Fluid> tag) implements Predicate<BlockState> {
    @Override
    public boolean test(BlockState state) {
        return !state.getFluidState().isEmpty() && state.getFluidState().is(tag);
    }
}
