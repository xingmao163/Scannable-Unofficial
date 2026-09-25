package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import com.starmao.scannable.api.ScanResultRenderContext;

public class ScanResultProviderItem extends AbstractScanResultProvider implements ScanResultProvider {
    private final List<ItemScanResult> results = new ArrayList<>();

    @Override public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo, float partialTicks, List<ScanResult> results) {}
    @Override public void computeScanResults() {}
    @Override public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) { results.forEach(callback::accept); }
    @Override public void reset() { results.clear(); }

    private static class ItemScanResult implements ScanResult {
        private final BlockPos pos; private final ItemStack stack; private final int count;
        ItemScanResult(BlockPos pos, ItemStack stack, int count) { this.pos = pos; this.stack = stack; this.count = count; }
        @Override public Vec3 getPosition() { return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5); }
        @Override @Nullable public AABB getRenderBounds() { return null; }
    }
}