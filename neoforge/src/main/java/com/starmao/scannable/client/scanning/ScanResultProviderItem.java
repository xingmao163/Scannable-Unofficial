package com.starmao.scannable.client.scanning;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.client.shader.Shaders;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import com.starmao.scannable.Scannable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Consumer;

/**
 * Scan result provider for the container item scanner module.
 *
 * <p>Renders container highlights using the same VBO-based approach as
 * {@link ScanResultProviderBlock}, with pre-baked geometry and per-face alpha.
 */
public final class ScanResultProviderItem extends AbstractScanResultProvider {
    private long renderStartTime;
    private long serverResultTime;

    private List<Item> targetItems = List.of();
    private final List<ItemScanResult> results = new ArrayList<>();
    private final List<ChunkSectionPos> pendingChunkSections = new ArrayList<>();
    private int currentChunkSection;
    private int chunkSectionsPerTick;

    // VBO cache: one VertexBuffer per container position, rebuilt on new results

    /**
     * Called by {@link com.starmao.scannable.client.ScanManager#setServerItemResults}
     * to initialise rendering state when server-side scan results arrive.
     * <p>Normal scan flow calls {@link #collectScanResults} which sets
     * {@code renderStartTime} — but item scan results bypass that path and
     * arrive directly from a network packet.
     */
    public void markResultsUpdated() {
        renderStartTime = System.currentTimeMillis();
        serverResultTime = renderStartTime;
    }
    private final Map<BlockPos, VertexBuffer> vboCache = new HashMap<>();
    private int lastRenderedResultCount = -1;

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);
        results.clear();
        pendingChunkSections.clear();
        targetItems = List.of();

        for (ItemStack stack : modules) {
            if (stack.getItem() instanceof ConfigurableItemScannerModuleItem moduleItem) {
                targetItems = moduleItem.getValues(stack);
                break;
            }
        }

        if (targetItems.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.scannable_unofficial.scanner.no_target_items"), true);
            }
            return;
        }

        BlockPos minPos = BlockPos.containing(center).offset(-this.radius, -this.radius, -this.radius);
        BlockPos maxPos = BlockPos.containing(center).offset(this.radius, this.radius, this.radius);
        ChunkPos minChunk = new ChunkPos(minPos);
        ChunkPos maxChunk = new ChunkPos(maxPos);

        int minSection = Math.max(player.level().getSectionIndex(minPos.getY()), 0);
        int maxSection = Math.min(player.level().getSectionIndex(maxPos.getY()),
                player.level().getSectionsCount() - 1);

        for (int sectionIdx = minSection; sectionIdx <= maxSection; sectionIdx++) {
            for (int cz = minChunk.z; cz <= maxChunk.z; cz++) {
                for (int cx = minChunk.x; cx <= maxChunk.x; cx++) {
                    int chunkY = player.level().getSectionYFromSectionIndex(sectionIdx);
                    double dx = Math.min(
                            Math.abs(new ChunkPos(cx, cz).getMinBlockX() - center.x),
                            Math.abs(new ChunkPos(cx, cz).getMaxBlockX() - center.x));
                    double dz = Math.min(
                            Math.abs(new ChunkPos(cx, cz).getMinBlockZ() - center.z),
                            Math.abs(new ChunkPos(cx, cz).getMaxBlockZ() - center.z));
                    double dy = Math.min(
                            Math.abs(SectionPos.sectionToBlockCoord(chunkY, 0) - center.y),
                            Math.abs(SectionPos.sectionToBlockCoord(chunkY, SectionPos.SECTION_MAX_INDEX) - center.y));
                    double sqDist = dx * dx + dy * dy + dz * dz;
                    if (sqDist > this.radius * this.radius) continue;
                    pendingChunkSections.add(new ChunkSectionPos(cx, cz, sectionIdx, sqDist));
                }
            }
        }

        pendingChunkSections.sort(Comparator.comparingDouble(p -> p.squareDistToCenter));
        chunkSectionsPerTick = Math.max(1, Mth.ceil(pendingChunkSections.size() / (float) scanTicks));
        currentChunkSection = 0;
    }

    @Override
    public void computeScanResults() {
        if (targetItems.isEmpty()) return;
        Level level = player.level();
        for (int i = 0; i < chunkSectionsPerTick; i++) {
            if (currentChunkSection >= pendingChunkSections.size()) return;
            ChunkSectionPos csp = pendingChunkSections.get(currentChunkSection);
            currentChunkSection++;

            ChunkAccess chunk = level.getChunk(csp.chunkX, csp.chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            LevelChunkSection[] sections = chunk.getSections();
            LevelChunkSection section = sections[csp.chunkSectionIndex];
            if (section == null || section.hasOnlyAir()) continue;

            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(csp.chunkSectionIndex));
            PalettedContainer<BlockState> palette = section.getStates();
            int originX = chunk.getPos().getWorldPosition().getX();
            int originZ = chunk.getPos().getWorldPosition().getZ();

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = palette.get(x, y, z);
                        if (state.isAir()) continue;
                        int gx = originX + x, gy = bottomY + y, gz = originZ + z;
                        BlockPos pos = new BlockPos(gx, gy, gz);
                        if (center.distanceToSqr(gx + 0.5, gy + 0.5, gz + 0.5) > this.radius * this.radius) continue;

                        IItemHandler itemHandler = null;
                        for (final var dir : net.minecraft.core.Direction.values()) {
                            itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
                            if (itemHandler != null) break;
                        }
                        if (itemHandler == null) continue;

                        java.util.Map<Item, Integer> itemCounts = new java.util.HashMap<>();
                        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                            ItemStack slotStack = itemHandler.getStackInSlot(slot);
                            if (slotStack.isEmpty()) continue;
                            if (isTargetItem(slotStack)) {
                                itemCounts.merge(slotStack.getItem(), slotStack.getCount(), Integer::sum);
                            }
                        }
                        for (var entry : itemCounts.entrySet()) {
                            results.add(new ItemScanResult(pos, entry.getKey().getDefaultInstance(), entry.getValue()));
                        }
                    }
                }
            }
        }
    }

    private boolean isTargetItem(ItemStack stack) {
        if (stack.isEmpty() || targetItems.isEmpty()) return false;
        for (Item target : targetItems) {
            if (target == stack.getItem()) return true;
        }
        return false;
    }

    @Override
    public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) {
        results.forEach(callback);
        renderStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                       Camera renderInfo, float partialTicks, List<ScanResult> results) {
        if (results.isEmpty()) return;
        switch (context) {
            case WORLD -> renderHighlights(poseStack, renderInfo, results);
            case GUI -> renderItemLabels(bufferSource, poseStack, renderInfo, results);
        }
    }

    @Override
    public void reset() {
        super.reset();
        targetItems = List.of();
        results.clear();
        invalidateVboCache();
        pendingChunkSections.clear();
        currentChunkSection = 0;
        chunkSectionsPerTick = 0;
    }

    // ========================================================================
    // VBO cache management
    // ========================================================================

    private void invalidateVboCache() {
        for (VertexBuffer vbo : vboCache.values()) {
            vbo.close();
        }
        vboCache.clear();
        // Force next renderHighlights to rebuild — reset() from setServerItemResults
        // clears the cache but the result count may be the same as the previous scan,
        // causing results.size() != lastRenderedResultCount to return false.
        lastRenderedResultCount = -1;
    }


    private void renderHighlights(PoseStack poseStack, Camera camera, List<ScanResult> results) {
        ShaderInstance shader = Shaders.getScanResultShader();
        if (shader == null) {
            if (ClientConfig.DEBUG_RENDER.get())
                Scannable.LOGGER.info("[ItemHighlight] scanResultShader is null, skipping");
            return;
        }

        shader.safeGetUniform("time").set((System.currentTimeMillis() - renderStartTime) / 1000.0f);

        // Rebuild VBO cache when the result list grows (the tick() wave adds
        // results incrementally to the same list object — identity-hash-based
        // detection would miss these additions).
        if (results.size() != lastRenderedResultCount) {
            if (ClientConfig.DEBUG_RENDER.get())
                Scannable.LOGGER.info("[ItemHighlight] VBO rebuild: {} results (was {}), {} VBOs cached",
                        results.size(), lastRenderedResultCount, vboCache.size());
            rebuildVboCache(results);
            lastRenderedResultCount = results.size();
        }

        if (ClientConfig.DEBUG_RENDER.get())
            Scannable.LOGGER.info("[ItemHighlight] Drawing {} VBO(s), poseStack valid={}, shader={}",
                    vboCache.size(), poseStack != null, shader != null);

        // Hand-depth trick: prevent overlay from rendering on top of held item
        renderHandDepth(poseStack, camera);

        RenderType renderType = getHighlightRenderLayer();
        renderType.setupRenderState();
        for (VertexBuffer vbo : vboCache.values()) {
            vbo.bind();
            vbo.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        }
        renderType.clearRenderState();
    }

    private int resolveColor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int color = state.getMapColor(level, pos).col;
        Integer override = ClientConfig.getBlockColor(state.getBlock());
        return override != null ? override : color;
    }

    private void rebuildVboCache(List<ScanResult> scanResults) {
        invalidateVboCache();
        Set<BlockPos> seen = new HashSet<>();
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            if (ClientConfig.DEBUG_RENDER.get())
                Scannable.LOGGER.warn("[ItemHighlight] rebuildVboCache: level is null");
            return;
        }
        for (ScanResult result : scanResults) {
            ItemScanResult ir = (ItemScanResult) result;
            if (!seen.add(ir.pos())) {
                if (ClientConfig.DEBUG_RENDER.get())
                    Scannable.LOGGER.info("[ItemHighlight]   skip duplicate: {}", ir.pos());
                continue;
            }
            int color = resolveColor(level, ir.pos());
            if (ClientConfig.DEBUG_RENDER.get())
                Scannable.LOGGER.info("[ItemHighlight]   VBO at {} color=#{}", ir.pos(), String.format("%06x", color));
            vboCache.put(ir.pos(), buildVbo(ir.pos(), color));
        }
    }

    private VertexBuffer buildVbo(BlockPos pos, int color) {
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        renderSingleBlockFaces(buffer, pos, color);
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vbo.bind();
        vbo.upload(buffer.buildOrThrow());
        VertexBuffer.unbind();
        return vbo;
    }


    private void renderHandDepth(PoseStack poseStack, Camera camera) {
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()
            || Minecraft.getInstance().options.hideGui
            || Minecraft.getInstance().gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR
            || Minecraft.getInstance().player == null) {
            if (ClientConfig.DEBUG_RENDER.get())
                Scannable.LOGGER.info("[HandDepth] Skipped (not first-person or hidden GUI or spectator)");
            return;
        }

        if (ClientConfig.DEBUG_RENDER.get())
            Scannable.LOGGER.info("[HandDepth] Enter — pushing modelView matrix");

        try {
            PoseStack viewPose = com.starmao.scannable.client.ScanManager.getWorldViewModelStack();
            if (viewPose == null) {
                if (ClientConfig.DEBUG_RENDER.get())
                    Scannable.LOGGER.warn("[HandDepth] worldViewModelStack is null");
                return;
            }

            RenderSystem.colorMask(false, false, false, false);
            org.joml.Matrix4f viewMat = new org.joml.Matrix4f(viewPose.last().pose());
            var mvStack = RenderSystem.getModelViewStack();
            mvStack.pushMatrix().mul(viewMat);
            PoseStack handPose = new PoseStack();
            handPose.pushPose();
            handPose.mulPose(viewMat.invert(new org.joml.Matrix4f()));
            var bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(256));
            Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderHandsWithItems(
                camera.getPartialTickTime(), handPose, bufferSource,
                (net.minecraft.client.player.LocalPlayer) Minecraft.getInstance().player,
                Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(Minecraft.getInstance().player, camera.getPartialTickTime())
            );
            bufferSource.endBatch();
            handPose.popPose();
            mvStack.popMatrix();
            if (ClientConfig.DEBUG_RENDER.get())
                Scannable.LOGGER.info("[HandDepth] Success — modelView restored");
        } catch (Throwable e) {
            Scannable.LOGGER.error("[HandDepth] Exception during hand render — modelView stack may be corrupted!", e);
        }
        RenderSystem.colorMask(true, true, true, true);
    }
    // ========================================================================
    // Geometry — matches ScanResultProviderBlock.BlockScanResult.render()
    // ========================================================================

    private static void renderSingleBlockFaces(VertexConsumer buffer, BlockPos pos, int color) {
        float cx = pos.getX(), cy = pos.getY(), cz = pos.getZ();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // Matches BlockScanResult.render() conventions: per-face alpha, UV 0..1
        // -X face (alpha 0.8)
        buffer.addVertex(cx, cy, cz).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx, cy, cz + 1).setUv(0, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx, cy + 1, cz).setUv(1, 0).setColor(r, g, b, 0.8f);
        // +X face (alpha 0.8)
        buffer.addVertex(cx + 1, cy, cz).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx + 1, cy + 1, cz).setUv(1, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx + 1, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx + 1, cy, cz + 1).setUv(0, 1).setColor(r, g, b, 0.8f);
        // -Y face (alpha 0.7)
        buffer.addVertex(cx, cy, cz).setUv(0, 0).setColor(r, g, b, 0.7f);
        buffer.addVertex(cx + 1, cy, cz).setUv(1, 0).setColor(r, g, b, 0.7f);
        buffer.addVertex(cx + 1, cy, cz + 1).setUv(1, 1).setColor(r, g, b, 0.7f);
        buffer.addVertex(cx, cy, cz + 1).setUv(0, 1).setColor(r, g, b, 0.7f);
        // +Y face (alpha 1.0)
        buffer.addVertex(cx, cy + 1, cz).setUv(0, 0).setColor(r, g, b, 1.0f);
        buffer.addVertex(cx, cy + 1, cz + 1).setUv(0, 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(cx + 1, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(cx + 1, cy + 1, cz).setUv(1, 0).setColor(r, g, b, 1.0f);
        // -Z face (alpha 0.9)
        buffer.addVertex(cx, cy, cz).setUv(0, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx, cy + 1, cz).setUv(0, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy + 1, cz).setUv(1, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy, cz).setUv(1, 0).setColor(r, g, b, 0.9f);
        // +Z face (alpha 0.9)
        buffer.addVertex(cx, cy, cz + 1).setUv(0, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy, cz + 1).setUv(1, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx, cy + 1, cz + 1).setUv(0, 1).setColor(r, g, b, 0.9f);
    }

    // ========================================================================
    // Rendering — GUI (item labels in world view)
    // ========================================================================

    private void renderItemLabels(MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo,
                                  List<ScanResult> results) {
        Vec3 lookVec = new Vec3(renderInfo.getLookVector());
        Vec3 viewerEyes = renderInfo.getPosition();
        float yaw = renderInfo.getYRot();
        float pitch = renderInfo.getXRot();
        boolean showDistance = renderInfo.getEntity().isShiftKeyDown();

        // Merge counts per position per item type (multi-direction scan may produce
        // duplicate entries for the same item at the same position).
        final java.util.Map<BlockPos, java.util.Map<ItemStack, Integer>> byPos = new java.util.LinkedHashMap<>();
        for (final ScanResult r : results) {
            final ItemScanResult ir = (ItemScanResult) r;
            byPos.computeIfAbsent(ir.pos(), k -> new java.util.LinkedHashMap<>())
                 .merge(ir.item(), ir.totalCount(), Integer::sum);
        }

        final java.util.List<ScanResult> deduped = new java.util.ArrayList<>();
        for (final var posEntry : byPos.entrySet()) {
            final BlockPos pos = posEntry.getKey();
            final java.util.Map<ItemStack, Integer> items = posEntry.getValue();
            final ItemStack firstItem = items.keySet().iterator().next();
            final int firstCount = items.get(firstItem);
            deduped.add(new ItemScanResult(pos, firstItem, firstCount));
        }

        deduped.sort(Comparator.comparing(r ->
                lookVec.dot(r.getPosition().subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes, showDistance, deduped,
                ScanResult::getPosition,
                result -> ModTextures.ICON_INFO,
                result -> {
                    final BlockPos resultPos = ((ItemScanResult) result).pos();
                    final java.util.Map<ItemStack, Integer> allItems = byPos.get(resultPos);
                    if (allItems == null) return Component.literal("");
                    if (allItems.size() == 1) {
                        final var entry = allItems.entrySet().iterator().next();
                        return Component.literal(entry.getKey().getHoverName().getString())
                                .append(Component.literal(" x" + entry.getValue()));
                    }
                    final StringBuilder sb = new StringBuilder();
                    for (final var entry : allItems.entrySet()) {
                        if (!sb.isEmpty()) sb.append(", ");
                        sb.append(entry.getKey().getHoverName().getString())
                          .append(" x").append(entry.getValue());
                    }
                    return Component.literal(sb.toString());
                },
                result -> true,
                MAX_ICONS, ICON_CONE_DOT);
    }

    // ========================================================================
    // Render layers
    // ========================================================================

    private static RenderType getHighlightRenderLayer() {
        return RenderType.create("item_scan_highlight",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 65536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(Shaders::getScanResultShader))
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .createCompositeState(false));
    }

    // ========================================================================
    // Data classes
    // ========================================================================

    private record ChunkSectionPos(int chunkX, int chunkZ, int chunkSectionIndex, double squareDistToCenter) {}
}
