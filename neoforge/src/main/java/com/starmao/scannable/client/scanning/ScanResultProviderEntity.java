package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import com.starmao.scannable.api.ScanResultRenderContext;

public class ScanResultProviderEntity extends AbstractScanResultProvider implements ScanResultProvider {
    private final List<ScanResultEntity> results = new ArrayList<>();

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo, float partialTicks, List<ScanResult> results) {
        if (results.isEmpty() || context != ScanResultRenderContext.WORLD) return;

        var fillConsumer = bufferSource.getBuffer(com.starmao.scannable.client.renderer.ScanResultRenderType.RESULT_BOX_TYPE);
        var shimmerConsumer = bufferSource.getBuffer(com.starmao.scannable.client.renderer.ScanResultRenderType.SHIMMER_TYPE);

        for (ScanResult result : results) {
            if (!(result instanceof ScanResultEntity sr)) continue;
            var box = sr.entity.getBoundingBox();
            drawBox(fillConsumer, poseStack,
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                0x44A8CCED);
            drawBox(shimmerConsumer, poseStack,
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                0xCCAACCED);
        }
    }
    @Override public void computeScanResults() {}
    @Override public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) { results.forEach(callback::accept); }
    @Override public void reset() { results.clear(); }

    private static class ScanResultEntity implements ScanResult {
        private final Entity entity;
        ScanResultEntity(Entity entity) { this.entity = entity; }
        @Override public Vec3 getPosition() { return entity.position(); }
        @Override @Nullable public AABB getRenderBounds() { return null; }
    }
}