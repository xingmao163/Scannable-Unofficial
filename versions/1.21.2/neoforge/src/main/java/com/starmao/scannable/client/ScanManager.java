package com.starmao.scannable.client;

import com.starmao.scannable.client.scanning.ItemScanResult;
import com.starmao.scannable.client.scanning.ScanResultProviders;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.ProjectionType;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.client.renderer.ScannerRenderer;
import com.starmao.scannable.Scannable;
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

import javax.annotation.Nullable;
import java.util.*;

public final class ScanManager {
    public static final int SCAN_COMPUTE_DURATION = 40;
    private static final int SCAN_INITIAL_RADIUS = 10;
    private static final int SCAN_TIME_OFFSET = 200;
    private static final int SCAN_GROWTH_DURATION = 2000;
    private static final int REFERENCE_RENDER_DISTANCE = 12;
    private static int getScanStayDuration() {
        return com.starmao.scannable.common.config.ModConfig.SCANNER_RESULT_STAY_DURATION.get();
    }

    private static final ByteBufferBuilder RENDER_BUFFER = new ByteBufferBuilder(256);

    private static float computeTargetRadius() {
        return Minecraft.getInstance().gameRenderer.getRenderDistance();
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
    private static final List<ScanResult> renderingList = new ArrayList<>();

    private static int scanningTicks = -1;
    private static long currentStart = -1;
    @Nullable private static Vec3 lastScanCenter;

    private static PoseStack worldViewModelStack;
    private static Matrix4f worldProjectionMatrix;

    public static PoseStack getWorldViewModelStack() {
        return worldViewModelStack;
    }

    // ---- Public API ---- //

    public static void beginScan(Player player, List<ItemStack> stacks) {
        cancelScan();

        float scanRadius = ModConfig.SCANNER_BASE_RADIUS.get();

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
            Scannable.LOGGER.info("[ScanManager] beginScan: NO providers registered (modules={})", stacks.size());
            return;
        }

        Vec3 center = player.position();
        Scannable.LOGGER.info("[ScanManager] beginScan: {} provider(s), radius={}, modules={}",
                collectingProviders.size(), scanRadius, stacks.size());
        for (ScanResultProvider provider : collectingProviders) {
            provider.initialize(player, stacks, center, scanRadius, SCAN_COMPUTE_DURATION);
            Scannable.LOGGER.info("[ScanManager] initialized provider: {}", provider.getClass().getSimpleName());
        }


    }

    /**
     * Inject server-side item scan results into the rendering pipeline.
     * Called when the client receives a {@link S2CItemScanResult} packet.
     * <p>
     * The results are placed directly into {@code pendingResults} so the
     * existing tick/render loop handles the expansion animation and display.
     */
    public static void setServerItemResults(final Vec3 center, final List<ItemScanResultData> rawResults) {
        if (center == null || rawResults == null || rawResults.isEmpty()) return;

        // Convert network data to renderable results
        final List<ScanResult> results = new ArrayList<>(rawResults.size());
        for (final ItemScanResultData data : rawResults) {
            final var itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(data.itemId());
            if (itemKey.isEmpty()) continue;
            results.add(new ItemScanResult(data.pos(), itemKey.get().getDefaultInstance(), data.totalCount()));
        }
        if (results.isEmpty()) return;

        // Clear previous item scan results so broken/moved containers don't persist.
        final ScanResultProvider provider = ScanResultProviders.ITEMS.get();
        pendingResults.remove(provider);
        synchronized (renderingResults) {
            final List<ScanResult> old = renderingResults.remove(provider);
            if (old != null) {
                provider.reset();
                old.forEach(ScanResult::close);
            }
        }

        lastScanCenter = center;
        currentStart = System.currentTimeMillis();

        // Initialize renderStartTime so the shader time uniform has a valid
        // starting point (collectScanResults is never called for item results).
        if (provider instanceof com.starmao.scannable.client.scanning.ScanResultProviderItem itemProvider) {
            itemProvider.markResultsUpdated();
        }

        pendingResults.put(provider, results);

        if (ModConfig.DEBUG_LOG_ITEM_SCANNER.get()) {
            com.starmao.scannable.Scannable.LOGGER.info("[ScanManager] Injected {} server item scan result(s)", results.size());
        }
    }

    public static void updateScan(Entity entity, boolean finish) {
        int remaining = SCAN_COMPUTE_DURATION - scanningTicks;

        if (!finish) {
            if (remaining <= 0) return;
            for (ScanResultProvider provider : collectingProviders) {
                provider.computeScanResults();
            }
            ++scanningTicks;
            return;
        }

        // Finish
        for (int i = 0; i < remaining; i++) {
            for (ScanResultProvider provider : collectingProviders) {
                provider.computeScanResults();
            }
        }

        int totalResults = 0;
        for (ScanResultProvider provider : collectingProviders) {
            List<ScanResult> collected = new ArrayList<>();
            provider.collectScanResults(entity.level(), collected::add);
            provider.reset();
            if (!collected.isEmpty()) {
                collectingResults.put(provider, collected);
                totalResults += collected.size();
                Scannable.LOGGER.info("[ScanManager] {} collected {} result(s)", provider.getClass().getSimpleName(), collected.size());
            }
        }
        Scannable.LOGGER.info("[ScanManager] updateScan finish: total {} result(s) from {} provider(s)",
                totalResults, collectingProviders.size());

        clear();


        lastScanCenter = Objects.requireNonNull(entity.position());
        currentStart = System.currentTimeMillis();

        pendingResults.putAll(collectingResults);
        pendingResults.values().forEach(list ->
                list.sort(Comparator.comparing(result -> -lastScanCenter.distanceTo(result.getPosition()))));

        // Immediately move all results to rendering so they're visible without
        // waiting for ClientTickEvent.Post to call tick(), which may not fire
        // reliably in all NeoForge versions (e.g. 1.21.2-beta).
        pendingResults.forEach((provider, results) -> {
            synchronized (renderingResults) {
                renderingResults.put(provider, new ArrayList<>(results));
            }
        });
        pendingResults.clear();

        Scannable.LOGGER.info("[ScanManager] renderingResults has {} entries", renderingResults.size());

        ScannerRenderer.INSTANCE.ping(lastScanCenter);
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
            // Fade out
            pendingResults.forEach((provider, results) -> results.forEach(ScanResult::close));
            pendingResults.clear();
            synchronized (renderingResults) {
                if (!renderingResults.isEmpty()) {
                    for (Iterator<Map.Entry<ScanResultProvider, List<ScanResult>>> it =
                         renderingResults.entrySet().iterator(); it.hasNext(); ) {
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

    public static void setMatrices(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix4f rotMatrix = new Matrix4f()
            .rotate(camera.rotation())
            .invert();
        worldViewModelStack = new PoseStack();
        worldViewModelStack.last().pose().set(rotMatrix);
        worldProjectionMatrix = projectionMatrix;
    }

    public static void renderLevel(float partialTick) {
        synchronized (renderingResults) {
            if (renderingResults.isEmpty()) return;
            Scannable.LOGGER.info("[renderLevel] {} providers, worldVMStack={}", renderingResults.size(), worldViewModelStack != null ? "set" : "null");
            render(ScanResultRenderContext.WORLD, partialTick, worldViewModelStack, worldProjectionMatrix);
        }
    }

    public static void renderGui(float partialTick) {
        synchronized (renderingResults) {
            if (renderingResults.isEmpty()) return;

            Scannable.LOGGER.info("[renderGui] {} providers in renderingResults, worldVMStack={}",
                    renderingResults.size(), worldViewModelStack != null ? "set" : "null");

            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(worldProjectionMatrix, ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();

            render(ScanResultRenderContext.GUI, partialTick, worldViewModelStack, worldProjectionMatrix);

            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void render(ScanResultRenderContext context, float partialTicks, PoseStack poseStack, Matrix4f projectionMatrix) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 pos = camera.getPosition();

        Frustum frustum = new Frustum(poseStack.last().pose(), projectionMatrix);
        frustum.prepare(pos.x(), pos.y(), pos.z());

        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        poseStack.pushPose();
        poseStack.translate(-pos.x, -pos.y, -pos.z);

        MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(RENDER_BUFFER);
        int totalRendered = 0;
        int totalAfterFrustum = 0;
        try {
            for (Map.Entry<ScanResultProvider, List<ScanResult>> entry : renderingResults.entrySet()) {
                for (ScanResult result : entry.getValue()) {
                    totalRendered++;
                    AABB bounds = result.getRenderBounds();
                    if (bounds == null || frustum.isVisible(bounds)) {
                        renderingList.add(result);
                        totalAfterFrustum++;
                    }
                }

                if (!renderingList.isEmpty()) {
                    entry.getKey().render(context, renderTypeBuffer, poseStack, camera, partialTicks, renderingList);
                    renderingList.clear();
                }
            }
        } finally {
            renderingList.clear();
        }

        Scannable.LOGGER.info("[render] context={}, totalResults={}, afterFrustum={}, mvStackTop={}", context, totalRendered, totalAfterFrustum, RenderSystem.getModelViewMatrix());

        renderTypeBuffer.endBatch();
        poseStack.popPose();

        RenderSystem.enableDepthTest();
    }


    @Nullable
    public static Vec3 getLastScanCenter() {
        return lastScanCenter;
    }

    public static float computeCurrentRadius() {
        if (currentStart < 0) return 0;
        return computeRadius(currentStart, (float) computeScanGrowthDuration());
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


}
