package com.starmao.scannable.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for a scan result provider.
 * Collects scan results over multiple ticks and renders them.
 */
public interface ScanResultProvider {
    void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks);

    void computeScanResults();

    void collectScanResults(BlockGetter level, Consumer<ScanResult> callback);

    void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                Camera renderInfo, float partialTicks, List<ScanResult> results);

    void reset();
}
