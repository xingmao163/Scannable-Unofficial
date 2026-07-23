package com.starmao.scannable.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.client.renderer.ScannerRenderer;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.item.ModuleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Central orchestrator for the client-side scan lifecycle on 26.1.2.
 * <p>
 * Manages scan start, tick-based result reveal, and world/GUI rendering
 * using the 26.1.2 RenderPipeline / MultiBufferSource / RenderType system.
 */
public final class ScanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanManager.class);

    public static final int SCAN_COMPUTE_DURATION = 40;
    private static final int SCAN_INITIAL_RADIUS = 10;
    private static final int SCAN_TIME_OFFSET = 200;
    private static final int SCAN_GROWTH_DURATION = 2000;
    private static final int REFERENCE_RENDER_DISTANCE = 12;

    private static int getScanStayDuration() {
        try {
            return ServerConfig.SCANNER_RESULT_STAY_DURATION.get();
        } catch (IllegalStateException e) {
            return 10000;
        }
    }

    private static final ByteBufferBuilder RENDER_BUFFER = new ByteBufferBuilder(256);

    private static float computeTargetRadius() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
    }

    public static int computeScanGrowthDuration() {
        return SCAN_GROWTH_DURATION * Minecraft.getInstance().options.renderDistance().get() / REFERENCE_RENDER_DISTANCE;
    }

    public static float computeRadius(long start, float duration) {
        float r1 = computeTargetRadius();
        float t1 = duration;
        float b = SCAN_TIME_OFFSET;
        float n = 1f / ((t1 + b) * (t1 + b) - b * b);
        float a = -r1 * b * b * n;
        float c = r1 * n;
        float t = (float) (System.currentTimeMillis() - start);
        return SCAN_INITIAL_RADIUS + a + (t + b) * (t + b) * c;
    }

    // ---- Scan state ---- //

    private static final Set<ScanResultProvider> collectingProviders = new HashSet<>();
    private static final Map<ScanResultProvider, List<ScanResult>> collectingResults = new HashMap<>();
    private static final Map<ScanResultProvider, List<ScanResult>> pendingResults = new HashMap<>();
    private static final Map<ScanResultProvider, List<ScanResult>> renderingResults = new HashMap<>();

    private static int scanningTicks = -1;
    private static long currentStart = -1;
    @Nullable private static Vec3 lastScanCenter;

    // ---- Public API ---- //

    public static void beginScan(Player player, List<ItemStack> stacks) {
        cancelScan();

        float scanRadius = ServerConfig.SCANNER_BASE_RADIUS.get();

        List<ScannerModule> modules = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ModuleHelper.getModule(stack).ifPresent(modules::add);
        }
        for (ScannerModule module : modules) {
            ScanResultProvider provider = module.getResultProvider();
            if (provider != null) {
                collectingProviders.add(provider);
            }
            scanRadius = module.adjustGlobalRange(scanRadius);
        }

        Vec3 center = player.position();

        for (ScanResultProvider provider : collectingProviders) {
            provider.initialize(player, stacks, center, scanRadius, SCAN_COMPUTE_DURATION);
        }

        scanningTicks = 0;
    }

    public static void updateScan(Entity entity, boolean finished) {
        if (!collectingProviders.isEmpty() && entity != null) {
            for (ScanResultProvider provider : collectingProviders) {
                provider.computeScanResults();
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (!collectingProviders.isEmpty() && mc.level != null) {
            for (ScanResultProvider provider : collectingProviders) {
                List<ScanResult> collected = new ArrayList<>();
                provider.collectScanResults(mc.level, collected::add);
                if (!collected.isEmpty()) {
                    collectingResults.put(provider, collected);
                }
            }
        }

        if (finished) {
            // Complete: move results to rendering
            for (ScanResultProvider provider : collectingProviders) {
                provider.reset();
            }

            lastScanCenter = entity != null ? entity.position() : null;
            currentStart = System.currentTimeMillis();

            pendingResults.putAll(collectingResults);
            pendingResults.values().forEach(list ->
                list.sort(Comparator.comparing(result ->
                    lastScanCenter != null ? -lastScanCenter.distanceTo(result.getPosition()) : 0)));

            synchronized (renderingResults) {
                pendingResults.forEach((provider, results) ->
                    renderingResults.put(provider, new ArrayList<>(results)));
            }
            pendingResults.clear();

            if (lastScanCenter != null) {
                ScannerRenderer.INSTANCE.ping(lastScanCenter);
            }
            cancelScan();
        }
    }

    public static void cancelScan() {
        collectingProviders.clear();
        collectingResults.clear();
        scanningTicks = 0;
    }

    public static void tick() {
        if (lastScanCenter == null || currentStart < 0) return;

        long elapsed = System.currentTimeMillis() - currentStart;
        if (elapsed > getScanStayDuration()) {
            pendingResults.forEach((provider, results) -> results.forEach(ScanResult::close));
            pendingResults.clear();
            synchronized (renderingResults) {
                if (!renderingResults.isEmpty()) {
                    Iterator<Map.Entry<ScanResultProvider, List<ScanResult>>> it =
                        renderingResults.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<ScanResultProvider, List<ScanResult>> entry = it.next();
                        List<ScanResult> list = entry.getValue();
                        for (int i = Mth.ceil(list.size() * 0.5f); i > 0; i--) {
                            list.remove(list.size() - 1).close();
                        }
                        if (list.isEmpty()) it.remove();
                    }
                }
                if (renderingResults.isEmpty()) {
                    clear();
                }
            }
            return;
        }

        if (pendingResults.isEmpty()) return;

        float radius = computeRadius(currentStart, computeScanGrowthDuration());
        float sqRadius = radius * radius;

        Iterator<Map.Entry<ScanResultProvider, List<ScanResult>>> it = pendingResults.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ScanResultProvider, List<ScanResult>> entry = it.next();
            ScanResultProvider provider = entry.getKey();
            List<ScanResult> results = entry.getValue();

            while (!results.isEmpty()) {
                int index = results.size() - 1;
                Vec3 position = results.get(index).getPosition();
                assert lastScanCenter != null;
                if (lastScanCenter.distanceToSqr(position) <= sqRadius) {
                    ScanResult result = results.remove(index);
                    synchronized (renderingResults) {
                        renderingResults.computeIfAbsent(provider, p -> new ArrayList<>()).add(result);
                    }
                } else {
                    break;
                }
            }
            if (results.isEmpty()) it.remove();
        }
    }

    public static void renderLevel(final PoseStack poseStack, final float partialTick) {
        synchronized (renderingResults) {
            if (renderingResults.isEmpty()) return;

            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 cam = camera.position();

            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);

            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(RENDER_BUFFER);

            for (Map.Entry<ScanResultProvider, List<ScanResult>> entry : renderingResults.entrySet()) {
                entry.getKey().render(ScanResultRenderContext.WORLD, bufferSource, poseStack, camera, partialTick, entry.getValue());
            }
            for (Map.Entry<ScanResultProvider, List<ScanResult>> entry : renderingResults.entrySet()) {
                entry.getKey().render(ScanResultRenderContext.GUI, bufferSource, poseStack, camera, partialTick, entry.getValue());
            }
            bufferSource.endBatch();

            poseStack.popPose();
        }
    }

    public static void renderGui(final float partialTick) {
        // GUI result overlay: currently unused in 26.1.2 (results render in world via renderLevel)
    }

    public static void setMatrices(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        // Not needed in 26.1.2 — renderLevel receives PoseStack from the render hook.
    }

    @Nullable
    public static Vec3 getLastScanCenter() {
        return lastScanCenter;
    }

    public static float computeCurrentRadius() {
        if (currentStart < 0) return 0;
        return computeRadius(currentStart, computeScanGrowthDuration());
    }

    // ---- Internal ---- //

    private static void clear() {
        pendingResults.clear();
        synchronized (renderingResults) {
            renderingResults.forEach((provider, results) -> {
                provider.reset();
                results.forEach(ScanResult::close);
            });
            renderingResults.clear();
        }
        lastScanCenter = null;
        currentStart = -1;
    }

    private ScanManager() {}
}
