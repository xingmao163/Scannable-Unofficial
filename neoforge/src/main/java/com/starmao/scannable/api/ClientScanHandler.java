package com.starmao.scannable.api;

import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Client-side scan handler contract.
 * <p>
 * Implemented in client-only code and registered during client setup.
 * Common code (e.g. {@link com.starmao.scannable.common.item.ScannerItem})
 * accesses the current implementation through {@link com.starmao.scannable.common.util.ClientAccessor}
 * without loading any client-only classes at the bytecode level.
 * <p>
 * All methods in this interface use only cross-platform parameter types
 * so the interface itself is safe to reference from common code.
 */
public interface ClientScanHandler {

    /**
     * Begin a scan (start scan progress).
     */
    void beginScan(Player player, List<ItemStack> modules);

    /**
     * Play the charging sound effect.
     */
    void playChargingSound();

    /**
     * Update an in-progress scan.
     *
     * @param entity   the scanning entity
     * @param finished whether the scan has completed
     */
    void updateScan(LivingEntity entity, boolean finished);

    /**
     * Cancel the current scan and stop charging sound.
     */
    void cancelScan();

    /**
     * Stop the charging sound without cancelling the scan.
     */
    void stopChargingSound();

    /**
     * Play the activation sound (scan complete).
     */
    void playActivateSound();

    /**
     * Set server-side item scan results for rendering on the client.
     *
     * @param center  the scan origin
     * @param results the discovered items
     */
    void setServerItemResults(Vec3 center, List<ItemScanResultData> results);
}
