package com.starmao.scannable.client;

import com.starmao.scannable.api.ClientScanHandler;
import com.starmao.scannable.client.audio.SoundManager;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Client-only implementation of {@link ClientScanHandler}.
 * <p>
 * Delegates to {@link ScanManager} and {@link SoundManager}.
 * This class is annotated {@link OnlyIn @OnlyIn(Dist.CLIENT)} so it is
 * safe to reference only from client-side initialisation code; it must
 * never be imported directly from common (server-side) classes.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientScanHandlerImpl implements ClientScanHandler {

    @Override
    public void beginScan(final Player player, final List<ItemStack> modules) {
        ScanManager.beginScan(player, modules);
    }

    @Override
    public void playChargingSound() {
        SoundManager.playChargingSound();
    }

    @Override
    public void updateScan(final LivingEntity entity, final boolean finished) {
        ScanManager.updateScan(entity, finished);
    }

    @Override
    public void cancelScan() {
        ScanManager.cancelScan();
        SoundManager.stopChargingSound();
    }

    @Override
    public void stopChargingSound() {
        SoundManager.stopChargingSound();
    }

    @Override
    public void playActivateSound() {
        SoundManager.playActivateSound();
    }

    @Override
    public void setServerItemResults(final Vec3 center, final List<ItemScanResultData> results) {
        ScanManager.setServerItemResults(center, results);
    }
}
