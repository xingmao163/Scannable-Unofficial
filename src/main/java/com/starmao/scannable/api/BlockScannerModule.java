package com.starmao.scannable.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


import java.util.function.Predicate;

/**
 * Specialization of ScannerModule for the built-in block scan result provider.
 * <p>
 * <strong>Client-only filtering.</strong> The {@link #getFilter(ItemStack)} method
 * is called during scan result computation on the client side. While the method
 * signature uses only cross-platform types, it should only be implemented in
 * modules that run client-side, where {@link ScanResultProvider} instances are
 * registered.
 *
 * @see ScanResultProviderRegistry
 */
public interface BlockScannerModule extends ScannerModule {

    /**
     * Adjusts the per-module scan radius multiplicatively.
     * <p>Unlike {@link #adjustGlobalRange(float)} which affects the overall
     * radius, this method adjusts the radius specifically for this module's
     * block scanning. Default returns the input unchanged.
     * <p>
     * <strong>Client-side.</strong> Called during scan result collection on the
     * client. Override only in modules that run client-side.
     *
     * @param range the base scan radius
     * @return the adjusted radius
     */
    default float adjustLocalRange(float range) {
        return range;
    }

    /**
     * Returns a predicate that filters which block states this module detects.
     * <p>
     * <strong>Client-only.</strong> Called during scan result computation on the
     * client. The predicate is evaluated against blocks in the world to determine
     * which ones are highlighted.
     *
     * @param module the specific item stack instance of this module
     * @return a predicate that returns {@code true} for detectable blocks
     */
    @OnlyIn(Dist.CLIENT)
    Predicate<BlockState> getFilter(ItemStack module);
}
