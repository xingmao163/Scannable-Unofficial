package com.starmao.scannable.api;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


/**
 * Interface for a scanner module.
 * <p>
 * Implement this to create a module that can be installed in the scanner.
 * Scanning behavior is <em>client side only</em>; only {@link #getEnergyCost}
 * and {@link #hasResultProvider} are called on the server.
 * <p>
 * Methods annotated with {@link OnlyIn @OnlyIn(Dist.CLIENT)} use client-only
 * classes and MUST NOT be called on a dedicated server. They are safe to call
 * on the client where providers are registered via
 * {@link ScanResultProviderRegistry}.
 */
public interface ScannerModule {

    /**
     * Returns the FE (Forge Energy) cost per scan tick for this module.
     * <p>Called on both client and server to compute total energy drain.
     * A return value of {@code 0} means the module is free to operate.
     *
     * @param module the specific item stack instance of this module
     * @return energy cost per tick, in FE
     */
    int getEnergyCost(ItemStack module);

    /**
     * Whether this module contributes a {@link ScanResultProvider} to the scan.
     * <p>Most modules return {@code true}. Override to {@code false} for
     * passive modules (e.g. range extenders) that adjust scan parameters
     * without producing their own results.
     *
     * @return {@code true} if this module provides scan results
     */
    default boolean hasResultProvider() {
        return true;
    }

    /**
     * Returns the scan result provider for this module.
     * <p>May return {@code null} if this module has no provider
     * (in which case {@link #hasResultProvider()} should also return {@code false}).
     * The returned provider is used client-side to collect and render scan results.
     * <p>
     * <strong>Client-only.</strong> The returned provider implements
     * {@link ScanResultProvider} which uses client-only rendering classes
     * ({@link com.mojang.blaze3d.vertex.PoseStack},
     * {@link net.minecraft.client.renderer.MultiBufferSource}, etc.).
     * This method is never called on a dedicated server.
     *
     * @return the provider, or {@code null} if this module produces no results
     */
    @Nullable
    @OnlyIn(Dist.CLIENT)
    ScanResultProvider getResultProvider();

    /**
     * Adjusts the global scan radius multiplicatively.
     * <p>Called for every installed module; the largest adjustment wins.
     * Default implementation returns the input range unchanged.
     * <p>
     * <strong>Conceptually client-side.</strong> This method is called during
     * scan setup which happens on the client. While the method signature itself
     * uses only cross-platform types, it should only be overridden in modules
     * that are used client-side.
     *
     * @param range the base scan radius determined by the scanner
     * @return the adjusted radius (e.g. {@code range * 1.5f} for +50 %)
     */
    default float adjustGlobalRange(float range) {
        return range;
    }
}
