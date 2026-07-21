package com.starmao.scannable.client.scanning;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starmao.scannable.Scannable;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.client.shader.Shaders;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Consumer;

/**
 * Scan result provider for the container item scanner module.
 *
 * <p>Renders container highlights using VBO caching to match ScanResultProviderBlock.
 */
public final class ScanResultProviderItem extends AbstractScanResultProvider {
    // ---- Scan state ---- //
    private long renderStartTime;
    private long serverResultTime;
    private List<Item> targetItems = List.of();
    private final List<ItemScanResult> results = new ArrayList<>();
    private final List<ChunkSectionPos> pendingChunkSections = new ArrayList<>();
    private int currentChunkSection;
    private int chunkSectionsPerTick;

    // ---- VBO cache ---- //
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
            if (player != null)
                player.displayClientMessage(Component.translatable("message.scannable_unofficial.scanner.no_target_items"), true);
            return;
        }

        BlockPos minPos = BlockPos.containing(center).offset(-(int) radius, -(int) radius, -(int) radius);
        BlockPos maxPos = BlockPos.containing(center).offset((int) radius, (int) radius, (int) radius);
        ChunkPos minChunk = new ChunkPos(minPos);
        ChunkPos maxChunk = new ChunkPos(maxPos);
        int minSection = Math.max(player.level().getSectionIndex(minPos.getY()), 0);
        int maxSection = Math.min(player.level().getSectionIndex(maxPos.getY()), player.level().getSectionsCount() - 1);

        for (int sectionIdx = minSection; sectionIdx <= maxSection; sectionIdx++) {
            for (int cz = minChunk.z; cz <= maxChunk.z; cz++) {
                for (int cx = minChunk.x; cx <= maxChunk.x; cx++) {
                    int chunkY = player.level().getSectionYFromSectionIndex(sectionIdx);
                    double dx = Math.min(Math.abs(new ChunkPos(cx, cz).getMinBlockX() - center.x),
                            Math.abs(new ChunkPos(cx, cz).getMaxBlockX() - center.x));
                    double dz = Math.min(Math.abs(new ChunkPos(cx, cz).getMinBlockZ() - center.z),
                            Math.abs(new ChunkPos(cx, cz).getMaxBlockZ() - center.z));
                    double dy = Math.min(Math.abs(SectionPos.sectionToBlockCoord(chunkY, 0) - center.y),
                            Math.abs(SectionPos.sectionToBlockCoord(chunkY, SectionPos.SECTION_MAX_INDEX) - center.y));
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    pendingChunkSections.add(new ChunkSectionPos(cx, cz, sectionIdx, dx * dx + dy * dy + dz * dz));
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
                        BlockPos pos = new BlockPos(originX + x, bottomY + y, originZ + z);
                        if (center.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > radius * radius)
                            continue;
                        IItemHandler itemHandler = null;
                        for (var dir : net.minecraft.core.Direction.values()) {
                            itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
                            if (itemHandler != null) break;
                        }
                        if (itemHandler == null) continue;
                        Map<Item, Integer> itemCounts = new HashMap<>();
                        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                            ItemStack slotStack = itemHandler.getStackInSlot(slot);
                            if (slotStack.isEmpty()) continue;
                            if (isTargetItem(slotStack))
                                itemCounts.merge(slotStack.getItem(), slotStack.getCount(), Integer::sum);
                        }
                        for (var entry : itemCounts.entrySet())
                            results.add(new ItemScanResult(pos, entry.getKey().getDefaultInstance(), entry.getValue()));
                    }
                }
            }
        }
    }

    private boolean isTargetItem(ItemStack stack) {
        if (stack.isEmpty() || targetItems.isEmpty()) return false;
        for (Item target : targetItems)
            if (target == stack.getItem()) return true;
        return false;
    }

    @Override
    public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) {
        results.forEach(callback);
        renderStartTime = System.currentTimeMillis();
    }

    /**
     * Mark the results as freshly updated (e.g. from server data).
     * Resets the render timer so the shader time uniform starts from zero.
     */
    public void markResultsUpdated() {
        renderStartTime = System.currentTimeMillis();
        serverResultTime = renderStartTime;
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
    // VBO cache
    // ========================================================================

    private void invalidateVboCache() {
        for (VertexBuffer vbo : vboCache.values()) vbo.close();
        vboCache.clear();
        lastRenderedResultCount = -1;
    }
    private void rebuildVboCache(List<ScanResult> results) {
        invalidateVboCache();
        Set<BlockPos> seen = new HashSet<>();
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        for (ScanResult result : results) {
            ItemScanResult ir = (ItemScanResult) result;
            if (!seen.add(ir.pos())) continue;
            BlockState state = level.getBlockState(ir.pos());
            int color = state.getMapColor(level, ir.pos()).col;
            Integer override = ClientConfig.getBlockColor(state.getBlock());
            if (override != null) color = override;
            vboCache.put(ir.pos(), buildVbo(ir.pos(), color));
        }
    }

    private VertexBuffer buildVbo(BlockPos pos, int color) {
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        renderSingleBlockFaces(buffer, pos, color);
        VertexBuffer vbo = new VertexBuffer(com.mojang.blaze3d.buffers.BufferUsage.DYNAMIC_WRITE);
        vbo.bind();
        vbo.upload(buffer.buildOrThrow());
        VertexBuffer.unbind();
        return vbo;
    }

    // ========================================================================
    // World rendering
    // ========================================================================

    private void renderHighlights(PoseStack poseStack, Camera camera, List<ScanResult> results) {
        CompiledShaderProgram shader = RenderSystem.getShader();
        if (shader == null) return;
        float t = (System.currentTimeMillis() - renderStartTime) / 1000.0f;
        float ts = ((float) Math.sin(t * 2.5) + 1.0f) * 0.5f;
        ts = ts * 0.15f + 0.85f;
        shader.safeGetUniform("time").set(t);
        shader.safeGetUniform("timeScale").set(ts);

        // Rebuild VBO cache when the result list grows (the tick() wave adds
        // results incrementally to the same list object — identity-hash-based
        // detection would miss these additions).
        if (results.size() != lastRenderedResultCount) {
            rebuildVboCache(results);
            lastRenderedResultCount = results.size();
        }

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

    @SuppressWarnings("null")
    private void renderHandDepth(PoseStack poseStack, Camera camera) {
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()
            || Minecraft.getInstance().options.hideGui
            || Minecraft.getInstance().gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR
            || Minecraft.getInstance().player == null) return;
        try {
            PoseStack viewPose = com.starmao.scannable.client.ScanManager.getWorldViewModelStack();
            if (viewPose == null) return;
            RenderSystem.colorMask(false, false, false, false);
            Matrix4f viewMat = new Matrix4f(viewPose.last().pose());
            var mvStack = RenderSystem.getModelViewStack();
            mvStack.pushMatrix().mul(viewMat);
            PoseStack handPose = new PoseStack();
            handPose.pushPose();
            handPose.mulPose(viewMat.invert(new Matrix4f()));
            var bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(256));
            Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderHandsWithItems(
                camera.getPartialTickTime(), handPose, bufferSource,
                (net.minecraft.client.player.LocalPlayer) Minecraft.getInstance().player,
                Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(Minecraft.getInstance().player, camera.getPartialTickTime()));
            bufferSource.endBatch();
            handPose.popPose();
            mvStack.popMatrix();
        } catch (Throwable e) { /* ignore */ }
        RenderSystem.colorMask(true, true, true, true);
    }

    // ========================================================================
    // Geometry — matches ScanResultProviderBlock
    // ========================================================================

    private static void renderSingleBlockFaces(VertexConsumer buffer, BlockPos pos, int color) {
        float cx = pos.getX(), cy = pos.getY(), cz = pos.getZ();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        // Matches BlockScanResult.render() per-face alpha
        // -X (0.8)
        buffer.addVertex(cx, cy, cz).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx, cy, cz + 1).setUv(0, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx, cy + 1, cz).setUv(1, 0).setColor(r, g, b, 0.8f);
        // +X (0.8)
        buffer.addVertex(cx + 1, cy, cz).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx + 1, cy + 1, cz).setUv(1, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx + 1, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(cx + 1, cy, cz + 1).setUv(0, 1).setColor(r, g, b, 0.8f);
        // -Y (0.7)
        buffer.addVertex(cx, cy, cz).setUv(0, 0).setColor(r, g, b, 0.7f);
        buffer.addVertex(cx + 1, cy, cz).setUv(1, 0).setColor(r, g, b, 0.7f);
        buffer.addVertex(cx + 1, cy, cz + 1).setUv(1, 1).setColor(r, g, b, 0.7f);
        buffer.addVertex(cx, cy, cz + 1).setUv(0, 1).setColor(r, g, b, 0.7f);
        // +Y (1.0)
        buffer.addVertex(cx, cy + 1, cz).setUv(0, 0).setColor(r, g, b, 1.0f);
        buffer.addVertex(cx, cy + 1, cz + 1).setUv(0, 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(cx + 1, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(cx + 1, cy + 1, cz).setUv(1, 0).setColor(r, g, b, 1.0f);
        // -Z (0.9)
        buffer.addVertex(cx, cy, cz).setUv(0, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx, cy + 1, cz).setUv(0, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy + 1, cz).setUv(1, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy, cz).setUv(1, 0).setColor(r, g, b, 0.9f);
        // +Z (0.9)
        buffer.addVertex(cx, cy, cz + 1).setUv(0, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy, cz + 1).setUv(1, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx + 1, cy + 1, cz + 1).setUv(1, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(cx, cy + 1, cz + 1).setUv(0, 1).setColor(r, g, b, 0.9f);
    }

    // ========================================================================
    // GUI rendering (item labels)
    // ========================================================================

    private void renderItemLabels(MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo,
                                  List<ScanResult> results) {
        Vec3 lookVec = new Vec3(renderInfo.getLookVector());
        Vec3 viewerEyes = renderInfo.getPosition();
        float yaw = renderInfo.getYRot();
        float pitch = renderInfo.getXRot();
        boolean showDistance = renderInfo.getEntity().isShiftKeyDown();

        // Merge counts per position per item type (multi-direction may produce duplicates)
        final Map<BlockPos, Map<ItemStack, Integer>> byPos = new LinkedHashMap<>();
        for (final ScanResult r : results) {
            final ItemScanResult ir = (ItemScanResult) r;
            byPos.computeIfAbsent(ir.pos(), k -> new LinkedHashMap<>())
                 .merge(ir.item(), ir.totalCount(), Integer::sum);
        }

        final List<ScanResult> deduped = new ArrayList<>();
        for (final var posEntry : byPos.entrySet()) {
            final BlockPos pos = posEntry.getKey();
            final Map<ItemStack, Integer> items = posEntry.getValue();
            final ItemStack firstItem = items.keySet().iterator().next();
            deduped.add(new ItemScanResult(pos, firstItem, items.get(firstItem)));
        }

        deduped.sort(Comparator.comparing(r ->
                lookVec.dot(r.getPosition().subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes, showDistance, deduped,
                ScanResult::getPosition,
                result -> ModTextures.ICON_INFO,
                result -> {
                    final BlockPos resultPos = ((ItemScanResult) result).pos();
                    final Map<ItemStack, Integer> allItems = byPos.get(resultPos);
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
                        .setShaderState(new RenderStateShard.ShaderStateShard(Shaders.SCAN_RESULT))
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .createCompositeState(false));
    }

    // ========================================================================
    // Data
    // ========================================================================

    private record ChunkSectionPos(int chunkX, int chunkZ, int chunkSectionIndex, double squareDistToCenter) {}
}
