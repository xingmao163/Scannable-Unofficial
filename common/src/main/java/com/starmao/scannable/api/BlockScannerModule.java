package com.starmao.scannable.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Specialization of ScannerModule for the built-in block scan result provider.
 */
public interface BlockScannerModule extends ScannerModule {
    default float adjustLocalRange(float range) {
        return range;
    }

    Predicate<BlockState> getFilter(ItemStack module);
}
