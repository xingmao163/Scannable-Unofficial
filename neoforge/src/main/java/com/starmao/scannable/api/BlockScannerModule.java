package com.starmao.scannable.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Specialization of ScannerModule for the built-in block scan result provider.
 */
public interface BlockScannerModule extends ScannerModule {

    /**
     * Adjusts the per-module scan radius multiplicatively.
     * <p>Unlike {@link #adjustGlobalRange(float)} which affects the overall
     * radius, this method adjusts the radius specifically for this module's
     * block scanning. Default returns the input unchanged.
     *
     * @param range the base scan radius
     * @return the adjusted radius
     */
    default float adjustLocalRange(float range) {
        return range;
    }

    /**
     * Returns a predicate that filters which block states this module detects.
     *
     * @param module the specific item stack instance of this module
     * @return a predicate that returns {@code true} for detectable blocks
     */
    Predicate<BlockState> getFilter(ItemStack module);
}
