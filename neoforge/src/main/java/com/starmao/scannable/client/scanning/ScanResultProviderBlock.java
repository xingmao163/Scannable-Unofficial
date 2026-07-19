package com.starmao.scannable.client.scanning;

import com.starmao.scannable.common.item.ModuleHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.client.shader.Shaders;
import com.starmao.scannable.common.scanning.filter.IgnoredBlocks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ScanResultProviderBlock extends AbstractScanResultProvider {
    private static final int MAX_RESULTS_PER_BLOCK = 8192;
    private static final int DEFAULT_COLOR = 0x4466CC;
    private final List<ScanFilterLayer> scanFilterLayers = new ArrayList<>();
    private final List<ChunkSectionPos> pendingChunkSections = new ArrayList<>();
    private int currentChunkSection, chunkSectionsPerTick;
    private final Map<Block, Map<BlockPos, BlockScanResult>> resultClusters = new HashMap<>();
    private final List<BlockScanResult> results = new ArrayList<>();
    private long renderStartTime;

    // ---- ScanResultProvider ---- //

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);

        scanFilterLayers.clear();
        resultClusters.clear();
        results.clear();

        Map<Integer, List<Predicate<BlockState>>> filterByRadius = new HashMap<>();
        for (ItemStack stack : modules) {
            Optional<ScannerModule> capability = ModuleHelper.getModule(stack);
            capability.ifPresent(module -> {
                if (module instanceof BlockScannerModule blockModule) {
                    Predicate<BlockState> filter = blockModule.getFilter(stack);
                    int localRadius = (int) Math.ceil(blockModule.adjustLocalRange(this.radius));
                    filterByRadius.computeIfAbsent(localRadius, r -> new ArrayList<>()).add(filter);
                }
            });
        }

        List<Integer> radii = new ArrayList<>(filterByRadius.keySet());
        radii.sort((a, b) -> -Integer.compare(a, b));

        if (!radii.isEmpty()) {
            this.radius = radii.get(0);
            for (int r : radii) {
                scanFilterLayers.add(new ScanFilterLayer(r, filterByRadius.get(r)));
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

                        if (sqDist > radius * radius) continue;

                        pendingChunkSections.add(new ChunkSectionPos(cx, cz, sectionIdx, sqDist));
                    }
                }
            }

            pendingChunkSections.sort(Comparator.comparingDouble(p -> p.squareDistToCenter));
            chunkSectionsPerTick = Mth.ceil(pendingChunkSections.size() / (float) scanTicks);
            currentChunkSection = 0;
        }
    }

    @Override
    public void computeScanResults() {
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
                if (IgnoredBlocks.contains(state)) continue;
                Block block = state.getBlock();
                Map<BlockPos, BlockScanResult> clusters = resultClusters.computeIfAbsent(block, b -> new HashMap<>());
                if (clusters.size() > MAX_RESULTS_PER_BLOCK) continue;

                int gx = originX + x;
                int gy = bottomY + y;
                int gz = originZ + z;

                double sqDist = center.distanceToSqr(gx + 0.5, gy + 0.5, gz + 0.5);

                outer:
                for (ScanFilterLayer layer : scanFilterLayers) {
                    if (sqDist > layer.radius * layer.radius) break;

                    for (Predicate<BlockState> filter : layer.filters) {
                        if (filter.test(state)) {
                            BlockPos pos = new BlockPos(gx, gy, gz);
                            if (!tryAddToCluster(clusters, pos)) {
                                BlockScanResult result = new BlockScanResult(block, pos);
                                clusters.put(pos, result);
                                results.add(result);
                            }
                            break outer;
                        }
                    }
                }
            }
        }
    }
}
}

    @Override
    public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) {
        for (BlockScanResult result : results) {
            if (result.isRoot()) {
                result.bake(level);
                callback.accept(result);
            }
        }
        renderStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                        Camera renderInfo, float partialTicks, List<ScanResult> results) {
        switch (context) {
            case WORLD -> renderBlocks(poseStack, renderInfo, partialTicks, results);
            case GUI -> renderBlockIcons(bufferSource, poseStack, renderInfo, results);
        }
    }

    @Override
    public void reset() {
        super.reset();
        scanFilterLayers.clear();
        currentChunkSection = chunkSectionsPerTick = 0;
        pendingChunkSections.clear();
        resultClusters.clear();
        results.clear();
    }

    // ---- Rendering ---- //

    public static RenderType getBlockScanResultRenderLayer() {
        return RenderType.create("scan_result",
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

    private void renderBlocks(PoseStack poseStack, Camera renderInfo, float partialTicks, List<ScanResult> results) {
        ShaderInstance shader = Shaders.getScanResultShader();
        if (shader == null) return;

        shader.safeGetUniform("time").set((System.currentTimeMillis() - renderStartTime) / 1000.0f);

        // Live hide-broken-blocks update: prune cells that no longer match and rebuild affected VBOs
        Level level = Minecraft.getInstance().level;
        Player viewer = Minecraft.getInstance().player;
        if (level != null) {
            for (ScanResult result : results) {
                BlockScanResult blockResult = (BlockScanResult) result;
                if (blockResult.needsLiveRefresh()) {
                    blockResult.refreshVisible(level, viewer);
                }
            }
        }

        RenderType renderType = getBlockScanResultRenderLayer();
        renderType.setupRenderState();
        for (ScanResult result : results) {
            BlockScanResult blockResult = (BlockScanResult) result;
            VertexBuffer vbo = blockResult.vbo;
            if (vbo == null) continue;
            vbo.bind();
            vbo.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        }
        renderType.clearRenderState();
    }

    private void renderBlockIcons(MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo,
                                   List<ScanResult> results) {
        Vec3 lookVec = new Vec3(renderInfo.getLookVector());
        Vec3 viewerEyes = renderInfo.getPosition();
        float yaw = renderInfo.getYRot();
        float pitch = renderInfo.getXRot();
        boolean showDistance = renderInfo.getEntity().isShiftKeyDown();

        results.sort(Comparator.comparing(result ->
                lookVec.dot(result.getPosition().subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes, showDistance, results,
                ScanResult::getPosition,
                result -> ModTextures.ICON_INFO,
                result -> ((BlockScanResult) result).block.getName(),
                result -> ((BlockScanResult) result).hasVisible(),
                Integer.MAX_VALUE, 0.98f);
    }

    // ---- Clustering ---- //

    private boolean tryAddToCluster(Map<BlockPos, BlockScanResult> clusters, BlockPos pos) {
        BlockScanResult root = null;
        root = tryAddToCluster(clusters, pos, pos.east(), root);
        root = tryAddToCluster(clusters, pos, pos.west(), root);
        root = tryAddToCluster(clusters, pos, pos.north(), root);
        root = tryAddToCluster(clusters, pos, pos.south(), root);
        root = tryAddToCluster(clusters, pos, pos.above(), root);
        root = tryAddToCluster(clusters, pos, pos.below(), root);
        return root != null;
    }

    @Nullable
    private BlockScanResult tryAddToCluster(Map<BlockPos, BlockScanResult> clusters, BlockPos pos,
                                             BlockPos clusterPos, @Nullable BlockScanResult root) {
        BlockScanResult cluster = clusters.get(clusterPos);
        if (cluster == null) return root;

        if (root == null) {
            root = cluster.getRoot();
            root.add(pos);
            clusters.put(pos, root);
        } else {
            cluster.getRoot().setRoot(root);
        }
        return root;
    }

    // ---- Data classes ---- //

    private record ScanFilterLayer(int radius, List<Predicate<BlockState>> filters) {}

    private record ChunkSectionPos(int chunkX, int chunkZ, int chunkSectionIndex, double squareDistToCenter) {}

    // ---- Scan Result implementation ---- //

    private static final class BlockScanResult implements ScanResult {
        private final Block block;
        private AABB bounds;
        @Nullable private BlockScanResult parent;
        private final Set<BlockPos> blocks;
        private int color;
        @Nullable private VertexBuffer vbo;
        @Nullable private Set<BlockPos> visibleBlocks;
        private long lastVisibleCheck;

        BlockScanResult(Block block, BlockPos pos) {
            this.block = block;
            bounds = new AABB(pos);
            blocks = new HashSet<>();
            blocks.add(pos);
        }

        void bake(BlockGetter level) {
            BlockState blockState = block.defaultBlockState();
            color = blockState.getMapColor(level, BlockPos.containing(bounds.getCenter())).col;
            if (color == 0) color = DEFAULT_COLOR;

            // Check ClientConfig color overrides
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                if (ClientConfig.getFluidColors().containsKey(BuiltInRegistries.FLUID.getKey(fluidState.getType()))) {
                    color = ClientConfig.getFluidColors().get(BuiltInRegistries.FLUID.getKey(fluidState.getType()));
                } else {
                    for (var entry : ClientConfig.getFluidTagColors().entrySet()) {
                        TagKey<net.minecraft.world.level.material.Fluid> tag = TagKey.create(net.minecraft.core.registries.Registries.FLUID, entry.getKey());
                        if (fluidState.is(tag)) {
                            color = entry.getValue();
                            break;
                        }
                    }
                }
            } else {
                if (ClientConfig.getBlockColors().containsKey(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()))) {
                    color = ClientConfig.getBlockColors().get(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
                } else {
                    for (var entry : ClientConfig.getBlockTagColors().entrySet()) {
                        TagKey<Block> tag = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, entry.getKey());
                        if (blockState.is(tag)) {
                            color = entry.getValue();
                            break;
                        }
                    }
                }
            }

            visibleBlocks = blocks;
            buildVbo();
        }

        boolean needsLiveRefresh() {
            return ClientConfig.HIDE_BROKEN_BLOCKS.get();
        }

        void refreshVisible(Level level, Player viewer) {
            if (visibleBlocks == null) return;
            // Only recheck periodically to avoid excessive chunk lookups
            long now = System.currentTimeMillis();
            if (now - lastVisibleCheck < 200) return;
            lastVisibleCheck = now;

            Set<BlockPos> present = new HashSet<>();
            for (BlockPos cell : blocks) {
                if (cellPresent(level, viewer, cell)) {
                    present.add(cell);
                }
            }
            if (present.equals(visibleBlocks)) return;
            visibleBlocks = present;
            if (present.isEmpty()) {
                if (vbo != null) {
                    vbo.close();
                    vbo = null;
                }
            } else {
                buildVbo();
            }
        }

        private boolean cellPresent(Level level, Player viewer, BlockPos cell) {
            if (!level.hasChunkAt(cell)) {
                return true; // Unloaded -> unknown, assume present.
            }
            return level.getBlockState(cell).is(block);
        }

        private void buildVbo() {
            if (visibleBlocks == null || visibleBlocks.isEmpty()) return;
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            render(buffer, new PoseStack());
            if (vbo == null) {
                vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            }
            vbo.bind();
            vbo.upload(buffer.buildOrThrow());
            VertexBuffer.unbind();
        }

        boolean hasVisible() {
            return visibleBlocks == null || !visibleBlocks.isEmpty();
        }

        boolean isRoot() {
            return parent == null;
        }

        BlockScanResult getRoot() {
            if (parent != null) return parent.getRoot();
            return this;
        }

        void setRoot(BlockScanResult root) {
            if (root == this) return;
            assert parent == null;
            root.bounds = root.bounds.minmax(bounds);
            root.blocks.addAll(blocks);
            blocks.clear();
            parent = root;
        }

        void add(BlockPos pos) {
            assert parent == null;
            bounds = bounds.minmax(new AABB(pos));
            blocks.add(pos);
        }

        void render(VertexConsumer buffer, PoseStack poseStack) {
            var matrix = poseStack.last().pose();

            float colorNormalizer = 1 / 255f;
            float r = ((color >> 16) & 0xFF) * colorNormalizer;
            float g = ((color >> 8) & 0xFF) * colorNormalizer;
            float b = (color & 0xFF) * colorNormalizer;

            float sizeUvX = (float) (1.0 / bounds.getXsize());
            float sizeUvY = (float) (1.0 / bounds.getYsize());
            float sizeUvZ = (float) (1.0 / bounds.getZsize());
            Set<BlockPos> cells = visibleBlocks != null ? visibleBlocks : blocks;
            for (BlockPos cell : cells) {
                // Render each exposed face with UV coordinates for coloring
                float cx = cell.getX(), cy = cell.getY(), cz = cell.getZ();

                // -X face
                if (!cells.contains(cell.offset(-1, 0, 0))) {
                    float u0 = (cy - (float) bounds.minY) * sizeUvY;
                    float u1 = u0 + sizeUvY;
                    float v0 = (cz - (float) bounds.minZ) * sizeUvZ;
                    float v1 = v0 + sizeUvZ;
                    buffer.addVertex(matrix, cx, cy, cz).setUv(u0, v0).setColor(r, g, b, 0.8f);
                    buffer.addVertex(matrix, cx, cy, cz + 1).setUv(u0, v1).setColor(r, g, b, 0.8f);
                    buffer.addVertex(matrix, cx, cy + 1, cz + 1).setUv(u1, v1).setColor(r, g, b, 0.8f);
                    buffer.addVertex(matrix, cx, cy + 1, cz).setUv(u1, v0).setColor(r, g, b, 0.8f);
                }
                // +X face
                if (!cells.contains(cell.offset(1, 0, 0))) {
                    float x2 = cx + 1;
                    float u0 = (cy - (float) bounds.minY) * sizeUvY;
                    float u1 = u0 + sizeUvY;
                    float v0 = (cz - (float) bounds.minZ) * sizeUvZ;
                    float v1 = v0 + sizeUvZ;
                    buffer.addVertex(matrix, x2, cy, cz).setUv(u0, v0).setColor(r, g, b, 0.8f);
                    buffer.addVertex(matrix, x2, cy + 1, cz).setUv(u1, v0).setColor(r, g, b, 0.8f);
                    buffer.addVertex(matrix, x2, cy + 1, cz + 1).setUv(u1, v1).setColor(r, g, b, 0.8f);
                    buffer.addVertex(matrix, x2, cy, cz + 1).setUv(u0, v1).setColor(r, g, b, 0.8f);
                }
                // -Y face
                if (!cells.contains(cell.offset(0, -1, 0))) {
                    float u0 = (cx - (float) bounds.minX) * sizeUvX;
                    float u1 = u0 + sizeUvX;
                    float v0 = (cz - (float) bounds.minZ) * sizeUvZ;
                    float v1 = v0 + sizeUvZ;
                    buffer.addVertex(matrix, cx, cy, cz).setUv(u0, v0).setColor(r, g, b, 0.7f);
                    buffer.addVertex(matrix, cx + 1, cy, cz).setUv(u1, v0).setColor(r, g, b, 0.7f);
                    buffer.addVertex(matrix, cx + 1, cy, cz + 1).setUv(u1, v1).setColor(r, g, b, 0.7f);
                    buffer.addVertex(matrix, cx, cy, cz + 1).setUv(u0, v1).setColor(r, g, b, 0.7f);
                }
                // +Y face
                if (!cells.contains(cell.offset(0, 1, 0))) {
                    float y2 = cy + 1;
                    float u0 = (cx - (float) bounds.minX) * sizeUvX;
                    float u1 = u0 + sizeUvX;
                    float v0 = (cz - (float) bounds.minZ) * sizeUvZ;
                    float v1 = v0 + sizeUvZ;
                    buffer.addVertex(matrix, cx, y2, cz).setUv(u0, v0).setColor(r, g, b, 1.0f);
                    buffer.addVertex(matrix, cx, y2, cz + 1).setUv(u0, v1).setColor(r, g, b, 1.0f);
                    buffer.addVertex(matrix, cx + 1, y2, cz + 1).setUv(u1, v1).setColor(r, g, b, 1.0f);
                    buffer.addVertex(matrix, cx + 1, y2, cz).setUv(u1, v0).setColor(r, g, b, 1.0f);
                }
                // -Z face
                if (!cells.contains(cell.offset(0, 0, -1))) {
                    float u0 = (cx - (float) bounds.minX) * sizeUvX;
                    float u1 = u0 + sizeUvX;
                    float v0 = (cy - (float) bounds.minY) * sizeUvY;
                    float v1 = v0 + sizeUvY;
                    buffer.addVertex(matrix, cx, cy, cz).setUv(u0, v0).setColor(r, g, b, 0.9f);
                    buffer.addVertex(matrix, cx, cy + 1, cz).setUv(u0, v1).setColor(r, g, b, 0.9f);
                    buffer.addVertex(matrix, cx + 1, cy + 1, cz).setUv(u1, v1).setColor(r, g, b, 0.9f);
                    buffer.addVertex(matrix, cx + 1, cy, cz).setUv(u1, v0).setColor(r, g, b, 0.9f);
                }
                // +Z face
                if (!cells.contains(cell.offset(0, 0, 1))) {
                    float z2 = cz + 1;
                    float u0 = (cx - (float) bounds.minX) * sizeUvX;
                    float u1 = u0 + sizeUvX;
                    float v0 = (cy - (float) bounds.minY) * sizeUvY;
                    float v1 = v0 + sizeUvY;
                    buffer.addVertex(matrix, cx, cy, z2).setUv(u0, v0).setColor(r, g, b, 0.9f);
                    buffer.addVertex(matrix, cx + 1, cy, z2).setUv(u1, v0).setColor(r, g, b, 0.9f);
                    buffer.addVertex(matrix, cx + 1, cy + 1, z2).setUv(u1, v1).setColor(r, g, b, 0.9f);
                    buffer.addVertex(matrix, cx, cy + 1, z2).setUv(u0, v1).setColor(r, g, b, 0.9f);
                }
            }
        }

        @Nullable
        @Override
        public AABB getRenderBounds() {
            return bounds;
        }

        @Override
        public Vec3 getPosition() {
            return bounds.getCenter();
        }

        @Override
        public void close() {
            if (vbo != null) {
                vbo.close();
                vbo = null;
            }
        }
    }
}
