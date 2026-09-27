package com.starmao.scannable.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.client.renderer.ScannerRenderer;
import com.starmao.scannable.client.scanning.ScanResultProviderItem;
import com.starmao.scannable.client.scanning.ScanResultProviders;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.common.network.data.ItemScanResultData;
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
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0f;
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

        if (collectingProviders.isEmpty()) {
            return;
        }

        Vec3 center = player.position();

        for (ScanResultProvider provider : collectingProviders) {
            provider.initialize(player, stacks, center, scanRadius, SCAN_COMPUTE_DURATION);
        }
    }

    public static void updateScan(Entity entity, boolean finished) {
        final int remaining = SCAN_COMPUTE_DURATION - scanningTicks;

        if (!finished) {
            if (remaining <= 0) {
                return;
            }

            if (!collectingProviders.isEmpty() && entity != null) {
                for (ScanResultProvider provider : collectingProviders) {
                    provider.computeScanResults();
                }
            }

            ++scanningTicks;
            return;
        }

        if (!collectingProviders.isEmpty() && entity != null) {
            for (int i = 0; i < remaining; i++) {
                for (ScanResultProvider provider : collectingProviders) {
                    provider.computeScanResults();
                }
            }
        }

        Minecraft mc = Minecraft.getInstance();
        for (ScanResultProvider provider : collectingProviders) {
            provider.collectScanResults(mc.level, result ->
                collectingResults.computeIfAbsent(provider, p -> new ArrayList<>()).add(result));
            provider.reset();
        }

        lastScanCenter = entity != null ? entity.position() : null;
        currentStart = System.currentTimeMillis();

        pendingResults.putAll(collectingResults);
        pendingResults.values().forEach(list ->
            list.sort(Comparator.comparing(result ->
                lastScanCenter != null ? -lastScanCenter.distanceTo(result.getPosition()) : 0)));

        if (lastScanCenter != null) {
            ScannerRenderer.INSTANCE.ping(lastScanCenter);
        }
        cancelScan();
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
            try {
                poseStack.translate(-cam.x, -cam.y, -cam.z);

                MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(RENDER_BUFFER);

                for (Map.Entry<ScanResultProvider, List<ScanResult>> entry : renderingResults.entrySet()) {
                    entry.getKey().render(ScanResultRenderContext.WORLD, bufferSource, poseStack, camera, partialTick, entry.getValue());
                }
                for (Map.Entry<ScanResultProvider, List<ScanResult>> entry : renderingResults.entrySet()) {
                    entry.getKey().render(ScanResultRenderContext.GUI, bufferSource, poseStack, camera, partialTick, entry.getValue());
                }
                bufferSource.endBatch();
            } finally {
                // A provider throwing mid-render must not leak the pushed pose or leave
                // uncommitted vertices in the shared RENDER_BUFFER, which would corrupt
                // every following frame rather than just this one.
                poseStack.popPose();
            }
        }
    }

    /**
     * No-op on 26.1.2: results are drawn in the world by {@link #renderLevel}, so there is no
     * separate GUI overlay pass. Kept because {@code ScannerClientSetup} — shared with the
     * 1.21.1 branch — still calls it every frame.
     */
    public static void renderGui(final float partialTick) {
        // Intentionally empty.
    }

    /**
     * Injects item scan results received from the server into the render pipeline.
     *
     * <p>Item results originate from a server-side scan
     * ({@code ItemScannerService}) and arrive via {@code S2CItemScanResult},
     * unlike block/entity results which are collected locally. They are pushed
     * into {@code pendingResults} so the existing reveal animation in
     * {@link #tick()} picks them up unchanged.
     *
     * @param center     scan centre, used as the origin of the reveal animation
     * @param rawResults container matches reported by the server
     */
    public static void setServerItemResults(final Vec3 center, final List<ItemScanResultData> rawResults) {
        if (rawResults.isEmpty()) return;

        final ScanResultProviderItem provider = ScanResultProviders.ITEMS.get();

        final List<ScanResult> converted = new ArrayList<>(rawResults.size());
        for (final ItemScanResultData data : rawResults) {
            converted.add(provider.createResult(data));
        }

        // tick() pops from the END of the list and reveals outward-in, so the
        // nearest result must sit at the end: sort ascending by negative distance.
        converted.sort(Comparator.comparing(
                result -> -center.distanceTo(result.getPosition())));

        // Clear previous item results first: a container may have been broken or
        // moved since the last scan, and renderingResults would otherwise keep
        // drawing its highlight until the stay duration expires.
        pendingResults.remove(provider);
        synchronized (renderingResults) {
            final List<ScanResult> old = renderingResults.remove(provider);
            if (old != null) {
                provider.reset();
                old.forEach(ScanResult::close);
            }
        }

        pendingResults.put(provider, converted);

        lastScanCenter = center;
        currentStart = System.currentTimeMillis();
        ScannerRenderer.INSTANCE.ping(center);
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
