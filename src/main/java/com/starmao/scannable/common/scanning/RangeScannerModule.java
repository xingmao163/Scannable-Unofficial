package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScannerModule;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Passive scanner module that increases the effective scan radius.
 * <p>Has no result provider ({@link #hasResultProvider()} returns {@code false})
 * — purely adjusts {@link #adjustGlobalRange} based on config values.
 * Singleton enum — stateless.
 */
public enum RangeScannerModule implements ScannerModule {
    INSTANCE;

    /**
     * {@return the energy cost in FE per scan tick, from the range booster config}
     */
    @Override
    public int getEnergyCost(ItemStack module) {
        return ServerConfig.SCANNER_ENERGY_COST_RANGE.get();
    }

    /**
     * {@code false} &mdash; this module does not produce scan results.
     *
     * @return {@code false}
     */
    @Override
    public boolean hasResultProvider() {
        return false;
    }

    /**
     * {@code null} &mdash; this module does not produce scan results.
     *
     * @return {@code null}
     */
    @Nullable
    @Override
    public ScanResultProvider getResultProvider() {
        return null;
    }

    /**
     * Adds the configured range bonus to the scanner's global base radius.
     *
     * @param range the current global range before modification
     * @return the boosted range after adding the range module's contribution
     */
    @Override
    public float adjustGlobalRange(float range) {
        return range + Mth.ceil(ServerConfig.SCANNER_BASE_RADIUS.get() * ServerConfig.SCANNER_RANGE_MODIFIER_RANGE.get());
    }
}
