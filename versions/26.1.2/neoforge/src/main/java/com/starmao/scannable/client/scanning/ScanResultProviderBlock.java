package com.starmao.scannable.client.scanning;

import com.starmao.scannable.common.item.ModuleHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import com.starmao.scannable.common.scanning.filter.IgnoredBlocks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
import net.minecraft.resources.Identifier;

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
            ChunkPos minChunk = new ChunkPos(minPos.getX() >> 4, minPos.getZ() >> 4);
            ChunkPos maxChunk = new ChunkPos(maxPos.getX() >> 4, maxPos.getZ() >> 4);

            int minSection = Math.max(player.level().getSectionIndex(minPos.getY()), 0);
            int maxSection = Math.min(player.level().getSectionIndex(maxPos.getY()),
                    player.level().getSectionsCount() - 1);

            for (int sectionIdx = minSection; sectionIdx <= maxSection; sectionIdx++) {
                for (int cz = minChunk.z(); cz <= maxChunk.z(); cz++) {
                    for (int cx = minChunk.x(); cx <= maxChunk.x(); cx++) {
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
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                        Camera renderInfo, float partialTicks, List<ScanResult> results) {
        if (context == ScanResultRenderContext.GUI) {
            renderBlockIcons(bufferSource, poseStack, renderInfo, results);
            return;
        }
        if (context != ScanResultRenderContext.WORLD) return;


        final PoseStack.Pose pose = poseStack.last();
        final VertexConsumer fill = bufferSource.getBuffer(ScanResultRenderType.SHIMMER_TYPE);
        for (ScanResult result : results) {
            final BlockScanResult br = (BlockScanResult) result;
            addBox(fill, pose, br.bounds, br.color);
        }
    }

    /** Render a colored box outline for the given AABB using the shimmer pipeline. */
    private static void addBox(VertexConsumer buffer, PoseStack.Pose pose, AABB bounds, int color) {
        final var matrix = pose.pose();
        final float r = ((color >> 16) & 0xFF) / 255.0f;
        final float g = ((color >> 8) & 0xFF) / 255.0f;
        final float b = (color & 0xFF) / 255.0f;

        final float minX = (float) bounds.minX, minY = (float) bounds.minY, minZ = (float) bounds.minZ;
        final float maxX = (float) bounds.maxX, maxY = (float) bounds.maxY, maxZ = (float) bounds.maxZ;

        // Bottom face (y = minY)
        buffer.addVertex(matrix, minX, minY, minZ).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, minX, minY, maxZ).setUv(0, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, maxX, minY, maxZ).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, maxX, minY, minZ).setUv(1, 0).setColor(r, g, b, 0.8f);

        // Top face (y = maxY)
        buffer.addVertex(matrix, minX, maxY, minZ).setUv(0, 0).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, maxX, maxY, minZ).setUv(1, 0).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setUv(1, 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, minX, maxY, maxZ).setUv(0, 1).setColor(r, g, b, 1.0f);

        // -X face (x = minX)
        buffer.addVertex(matrix, minX, minY, minZ).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, minX, maxY, minZ).setUv(1, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, minX, maxY, maxZ).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, minX, minY, maxZ).setUv(0, 1).setColor(r, g, b, 0.8f);

        // +X face (x = maxX)
        buffer.addVertex(matrix, maxX, minY, minZ).setUv(0, 0).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, maxX, minY, maxZ).setUv(0, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setUv(1, 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, maxX, maxY, minZ).setUv(1, 0).setColor(r, g, b, 0.8f);

        // -Z face (z = minZ)
        buffer.addVertex(matrix, minX, minY, minZ).setUv(0, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, minX, maxY, minZ).setUv(0, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, maxX, maxY, minZ).setUv(1, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, maxX, minY, minZ).setUv(1, 0).setColor(r, g, b, 0.9f);

        // +Z face (z = maxZ)
        buffer.addVertex(matrix, minX, minY, maxZ).setUv(0, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, maxX, minY, maxZ).setUv(1, 0).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setUv(1, 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, minX, maxY, maxZ).setUv(0, 1).setColor(r, g, b, 0.9f);
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

    // ---- Rendering -- GUI ---- //

    private void renderBlockIcons(MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo,
                                   List<ScanResult> results) {
        Vec3 lookVec = new Vec3(renderInfo.forwardVector());
        Vec3 viewerEyes = renderInfo.position();
        float yaw = renderInfo.yRot();
        float pitch = renderInfo.xRot();
        boolean showDistance = renderInfo.entity().isShiftKeyDown();

        results.sort(Comparator.comparing(result ->
                lookVec.dot(result.getPosition().subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes, showDistance, results,
                ScanResult::getPosition,
                result -> com.starmao.scannable.Scannable.id("textures/gui/overlay/info.png"),
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

    @SuppressWarnings("null")
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
        int color;

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

        }




        boolean hasVisible() {
            return true;
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
            // No VBO to close; rendering is handled per-frame via ScanResultRenderType.SHIMMER_TYPE
        }
    }
}
