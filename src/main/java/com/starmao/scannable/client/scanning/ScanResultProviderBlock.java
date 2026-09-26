package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.common.scanning.filter.IgnoredBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ScanResultProviderBlock extends AbstractScanResultProvider implements ScanResultProvider {
    private static final int MAX_RESULTS_PER_BLOCK = 8192;
    private static final int DEFAULT_COLOR = 0x4466CC;
    private static final int MAX_CONTOUR_CELLS = 256;

    private final List<ScanFilterLayer> scanFilterLayers = new ArrayList<>();
    private final List<ChunkSectionPos> pendingChunkSections = new ArrayList<>();
    private int currentChunkSection, chunkSectionsPerTick;
    private final Map<Block, Map<BlockPos, BlockScanResult>> resultClusters = new HashMap<>();
    private final List<BlockScanResult> results = new ArrayList<>();

    // ----------------------------------------------------------------- //

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);
        this.scanFilterLayers.clear();
        this.pendingChunkSections.clear();
        this.resultClusters.clear();
        this.results.clear();

        Map<Integer, List<Predicate<BlockState>>> filterByRadius = new LinkedHashMap<>();
        for (ItemStack stack : modules) {
            ModuleHelper.getModule(stack).ifPresent(module -> {
                if (module instanceof BlockScannerModule bm) {
                    Predicate<BlockState> filter = bm.getFilter(stack);
                    if (filter != null) {
                        int localRadius = (int) Math.ceil(bm.adjustLocalRange(this.radius));
                        filterByRadius.computeIfAbsent(localRadius, r -> new ArrayList<>()).add(filter);
                    }
                }
            });
        }

        List<Integer> sortedRadii = new ArrayList<>(filterByRadius.keySet());
        sortedRadii.sort((a, b) -> -Integer.compare(a, b));

        if (sortedRadii.isEmpty()) {
            return;
        }

        this.radius = sortedRadii.get(0);
        for (int r : sortedRadii) {
            scanFilterLayers.add(new ScanFilterLayer(r, filterByRadius.get(r)));
        }

        BlockPos minBlockPos = BlockPos.containing(center).offset(-this.radius, -this.radius, -this.radius);
        BlockPos maxBlockPos = BlockPos.containing(center).offset(this.radius, this.radius, this.radius);
        ChunkPos minChunkPos = new ChunkPos(minBlockPos.getX() >> 4, minBlockPos.getZ() >> 4);
        ChunkPos maxChunkPos = new ChunkPos(maxBlockPos.getX() >> 4, maxBlockPos.getZ() >> 4);

        int minSec = Math.max(player.level().getSectionIndex(minBlockPos.getY()), 0);
        int maxSec = Math.min(player.level().getSectionIndex(maxBlockPos.getY()), player.level().getSectionsCount() - 1);

        for (int si = minSec; si <= maxSec; si++) {
            for (int cz = minChunkPos.z(); cz <= maxChunkPos.z(); cz++) {
                for (int cx = minChunkPos.x(); cx <= maxChunkPos.x(); cx++) {
                    int cy = player.level().getSectionYFromSectionIndex(si);
                    double dx = Math.min(
                        Math.abs((cx << 4) - center.x),
                        Math.abs(((cx + 1) << 4) - center.x));
                    double dz = Math.min(
                        Math.abs((cz << 4) - center.z),
                        Math.abs(((cz + 1) << 4) - center.z));
                    double minY = SectionPos.sectionToBlockCoord(cy, 0);
                    double maxY = SectionPos.sectionToBlockCoord(cy, 15);
                    double dy = Math.min(
                        Math.abs(minY - center.y),
                        Math.abs(maxY - center.y));
                    double sqDist = dx * dx + dy * dy + dz * dz;
                    if (sqDist > (double) this.radius * this.radius) {
                        continue;
                    }
                    pendingChunkSections.add(new ChunkSectionPos(cx, cz, si, sqDist));
                }
            }
        }

        pendingChunkSections.sort(Comparator.comparingDouble(p -> p.squareDistToCenter));
        chunkSectionsPerTick = Mth.ceil(pendingChunkSections.size() / (float) scanTicks);
        this.currentChunkSection = 0;
    }

    // ----------------------------------------------------------------- //

    @Override
    public void computeScanResults() {
        Level level = player.level();
        for (int i = 0; i < chunkSectionsPerTick; i++) {
            if (currentChunkSection >= pendingChunkSections.size()) {
                return;
            }
            ChunkSectionPos cps = pendingChunkSections.get(currentChunkSection);
            currentChunkSection++;

            ChunkAccess chunk = level.getChunk(cps.chunkX, cps.chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) {
                continue;
            }

            LevelChunkSection section = chunk.getSections()[cps.chunkSectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            int bottomY = SectionPos.sectionToBlockCoord(
                chunk.getSectionYFromSectionIndex(cps.chunkSectionIndex));
            PalettedContainer<BlockState> palette = section.getStates();
            int originX = chunk.getPos().getWorldPosition().getX();
            int originY = bottomY;
            int originZ = chunk.getPos().getWorldPosition().getZ();

            for (int idx = 0; idx < 16 * 16 * 16; idx++) {
                BlockState state = palette.get(idx);
                Block block = state.getBlock();
                Map<BlockPos, BlockScanResult> clusters = resultClusters.computeIfAbsent(block, b -> new HashMap<>());
                if (clusters.size() > MAX_RESULTS_PER_BLOCK) {
                    continue;
                }
                if (IgnoredBlocks.contains(state)) {
                    continue;
                }

                int x = idx & 0xf;
                int z = (idx >> 4) & 0xf;
                int y = (idx >> 8) & 0xf;
                int gx = originX + x;
                int gy = originY + y;
                int gz = originZ + z;
                double sqDist = center.distanceToSqr(gx + 0.5, gy + 0.5, gz + 0.5);

                outer:
                for (ScanFilterLayer layer : scanFilterLayers) {
                    if (sqDist > (double) layer.radius * layer.radius) {
                        break;
                    }
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
    public void render(ScanResultRenderContext ctx, MultiBufferSource buf, PoseStack pose, Camera cam, float pt, List<ScanResult> results) {
        if (results.isEmpty()) return;

        if (ctx == ScanResultRenderContext.WORLD) {
            renderBlockWorld(buf, pose, cam, pt, results);
        } else if (ctx == ScanResultRenderContext.GUI) {
            renderBlockIcons(buf, pose, cam, pt, results);
        }
    }

    private void renderBlockWorld(MultiBufferSource buf, PoseStack pose, Camera cam, float pt, List<ScanResult> results) {
        final PoseStack.Pose poseEntry = pose.last();
        VertexConsumer fill = buf.getBuffer(ScanResultRenderType.SHIMMER_TYPE);

        for (ScanResult result : results) {
            BlockScanResult br = (BlockScanResult) result;
            int c = br.color;
            float r = ((c >> 16) & 0xFF) / 255.0f;
            float g = ((c >> 8) & 0xFF) / 255.0f;
            float b = (c & 0xFF) / 255.0f;
            if (br.blocks.size() <= MAX_CONTOUR_CELLS) {
                Set<BlockPos> cells = visibleCells(br);
                if (cells.isEmpty()) {
                    continue;
                }
                addClusterFill(fill, poseEntry, cells, br.bounds, r, g, b, 1.0f);
            } else {
                addBox(fill, poseEntry, br.bounds, r, g, b, 1.0f);
            }
        }
    }

    private void renderBlockIcons(MultiBufferSource buf, PoseStack pose, Camera cam, float pt, List<ScanResult> results) {
        org.joml.Vector3fc forward = cam.forwardVector();
        Vec3 lookVec = new Vec3(forward.x(), forward.y(), forward.z());
        Vec3 viewerEyes = cam.position();
        float yaw = cam.yRot();
        float pitch = cam.xRot();
        Player player = Minecraft.getInstance().player;
        boolean showDistance = player != null && player.isShiftKeyDown();

        // Sort ascending by how closely each result is aimed at, so the best-aimed results sit at
        // the END of the list — the loop below walks backwards from there.
        results.sort(Comparator.comparing((ScanResult result) -> {
            Vec3 pos = result.getPosition();
            return lookVec.dot(pos.subtract(viewerEyes).normalize());
        }));

        // Walk from the best-aimed result backwards, drawing at most MAX_ICONS of them. This
        // mirrors renderIconLabels: it caps how many icons can pile up on screen and gives the
        // limited slots to whatever the player is most likely looking at.
        int shown = 0;
        for (int i = results.size() - 1; i >= 0 && shown < MAX_ICONS; i--) {
            BlockScanResult br = (BlockScanResult) results.get(i);
            if (!hasVisibleCells(br)) {
                continue;
            }
            // No look-direction gate here: renderIconLabel draws the icon for every visible
            // result and gates only the text label on the crosshair, matching the 1.21.1 overlay.
            // Filtering on the look vector at this level would hide the icons too, making a scan
            // appear to have found nothing until the player aims at each result.
            Vec3 pos = br.getPosition();
            Component label = br.label != null ? br.label : br.block.getName();
            float distance = showDistance ? (float) pos.subtract(viewerEyes).length() : 0f;
            renderIconLabel(buf, pose, yaw, pitch, lookVec, viewerEyes, distance, pos,
                    ModTextures.ICON_INFO, label);
            shown++;
        }
    }

    private static Set<BlockPos> visibleCells(BlockScanResult br) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return br.blocks;
        }
        Set<BlockPos> visible = new HashSet<>();
        for (BlockPos cell : br.blocks) {
            if (!level.hasChunkAt(cell) || level.getBlockState(cell).is(br.block)) {
                visible.add(cell);
            }
        }
        return visible;
    }

    private static boolean hasVisibleCells(BlockScanResult br) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return true;
        }
        for (BlockPos cell : br.blocks) {
            if (!level.hasChunkAt(cell) || level.getBlockState(cell).is(br.block)) {
                return true;
            }
        }
        return false;
    }

    // -- Drawing -----------------------------------------------------------

    private static void addBox(VertexConsumer buf, PoseStack.Pose pose, AABB box, float r, float g, float b, float a) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        vertex(buf, pose, x0, y0, z0, 0, 0, r, g, b, a); vertex(buf, pose, x0, y0, z1, 0, 1, r, g, b, a);
        vertex(buf, pose, x0, y1, z1, 1, 1, r, g, b, a); vertex(buf, pose, x0, y1, z0, 1, 0, r, g, b, a);
        vertex(buf, pose, x1, y0, z1, 0, 1, r, g, b, a); vertex(buf, pose, x1, y0, z0, 0, 0, r, g, b, a);
        vertex(buf, pose, x1, y1, z0, 1, 0, r, g, b, a); vertex(buf, pose, x1, y1, z1, 1, 1, r, g, b, a);
        vertex(buf, pose, x0, y0, z0, 0, 0, r, g, b, a); vertex(buf, pose, x1, y0, z0, 1, 0, r, g, b, a);
        vertex(buf, pose, x1, y0, z1, 1, 1, r, g, b, a); vertex(buf, pose, x0, y0, z1, 0, 1, r, g, b, a);
        vertex(buf, pose, x0, y1, z1, 0, 1, r, g, b, a); vertex(buf, pose, x1, y1, z1, 1, 1, r, g, b, a);
        vertex(buf, pose, x1, y1, z0, 1, 0, r, g, b, a); vertex(buf, pose, x0, y1, z0, 0, 0, r, g, b, a);
        vertex(buf, pose, x1, y0, z0, 1, 0, r, g, b, a); vertex(buf, pose, x0, y0, z0, 0, 0, r, g, b, a);
        vertex(buf, pose, x0, y1, z0, 0, 1, r, g, b, a); vertex(buf, pose, x1, y1, z0, 1, 0, r, g, b, a);
        vertex(buf, pose, x0, y0, z1, 0, 0, r, g, b, a); vertex(buf, pose, x1, y0, z1, 1, 0, r, g, b, a);
        vertex(buf, pose, x1, y1, z1, 1, 1, r, g, b, a); vertex(buf, pose, x0, y1, z1, 0, 1, r, g, b, a);
    }

    private static void addClusterFill(VertexConsumer buf, PoseStack.Pose pose, Set<BlockPos> cells,
                                       AABB bounds, float r, float g, float b, float a) {
        float sx = 1.0f / (float) bounds.getXsize();
        float sy = 1.0f / (float) bounds.getYsize();
        float sz = 1.0f / (float) bounds.getZsize();
        float bx = (float) bounds.minX, by = (float) bounds.minY, bz = (float) bounds.minZ;
        for (BlockPos cell : cells) {
            float x0 = cell.getX(), y0 = cell.getY(), z0 = cell.getZ();
            float x1 = x0 + 1.0f, y1 = y0 + 1.0f, z1 = z0 + 1.0f;
            float ux0 = (x0 - bx) * sx, ux1 = (x1 - bx) * sx;
            float uy0 = (y0 - by) * sy, uy1 = (y1 - by) * sy;
            float uz0 = (z0 - bz) * sz, uz1 = (z1 - bz) * sz;
            if (!cells.contains(cell.west())) {
                vertex(buf, pose, x0, y0, z0, uy0, uz0, r, g, b, a);
                vertex(buf, pose, x0, y0, z1, uy0, uz1, r, g, b, a);
                vertex(buf, pose, x0, y1, z1, uy1, uz1, r, g, b, a);
                vertex(buf, pose, x0, y1, z0, uy1, uz0, r, g, b, a);
            }
            if (!cells.contains(cell.east())) {
                vertex(buf, pose, x1, y0, z1, uy0, uz1, r, g, b, a);
                vertex(buf, pose, x1, y0, z0, uy0, uz0, r, g, b, a);
                vertex(buf, pose, x1, y1, z0, uy1, uz0, r, g, b, a);
                vertex(buf, pose, x1, y1, z1, uy1, uz1, r, g, b, a);
            }
            if (!cells.contains(cell.below())) {
                vertex(buf, pose, x0, y0, z0, ux0, uz0, r, g, b, a);
                vertex(buf, pose, x1, y0, z0, ux1, uz0, r, g, b, a);
                vertex(buf, pose, x1, y0, z1, ux1, uz1, r, g, b, a);
                vertex(buf, pose, x0, y0, z1, ux0, uz1, r, g, b, a);
            }
            if (!cells.contains(cell.above())) {
                vertex(buf, pose, x0, y1, z1, ux0, uz1, r, g, b, a);
                vertex(buf, pose, x1, y1, z1, ux1, uz1, r, g, b, a);
                vertex(buf, pose, x1, y1, z0, ux1, uz0, r, g, b, a);
                vertex(buf, pose, x0, y1, z0, ux0, uz0, r, g, b, a);
            }
            if (!cells.contains(cell.north())) {
                vertex(buf, pose, x1, y0, z0, ux1, uy0, r, g, b, a);
                vertex(buf, pose, x0, y0, z0, ux0, uy0, r, g, b, a);
                vertex(buf, pose, x0, y1, z0, ux0, uy1, r, g, b, a);
                vertex(buf, pose, x1, y1, z0, ux1, uy1, r, g, b, a);
            }
            if (!cells.contains(cell.south())) {
                vertex(buf, pose, x0, y0, z1, ux0, uy0, r, g, b, a);
                vertex(buf, pose, x1, y0, z1, ux1, uy0, r, g, b, a);
                vertex(buf, pose, x1, y1, z1, ux1, uy1, r, g, b, a);
                vertex(buf, pose, x0, y1, z1, ux0, uy1, r, g, b, a);
            }
        }
    }

    private static void vertex(VertexConsumer buf, PoseStack.Pose pose, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        buf.addVertex(pose, x, y, z).setUv(u, v).setColor(r, g, b, a);
    }

    // -- Clustering --------------------------------------------------------

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
    private BlockScanResult tryAddToCluster(Map<BlockPos, BlockScanResult> clusters,
                                             BlockPos pos, BlockPos clusterPos, @Nullable BlockScanResult root) {
        BlockScanResult cluster = clusters.get(clusterPos);
        if (cluster == null) {
            return root;
        }
        if (root == null) {
            root = cluster.getRoot();
            root.add(pos);
            clusters.put(pos, root);
        } else {
            cluster.getRoot().setRoot(root);
        }
        return root;
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

    // -- Inner types -------------------------------------------------------

    private record ScanFilterLayer(int radius, List<Predicate<BlockState>> filters) { }

    private record ChunkSectionPos(int chunkX, int chunkZ, int chunkSectionIndex, double squareDistToCenter) { }

    private static final class BlockScanResult implements ScanResult {
        private final Block block;
        private AABB bounds;
        @Nullable private BlockScanResult parent;
        private final Set<BlockPos> blocks;
        private int color;
        @Nullable private Component label;

        BlockScanResult(Block block, BlockPos pos) {
            this.block = block;
            this.bounds = new AABB(pos);
            this.blocks = new HashSet<>();
            this.blocks.add(pos);
        }

        void bake(BlockGetter level) {
            BlockState state = block.defaultBlockState();
            int mapColor = state.getMapColor(level, BlockPos.containing(bounds.getCenter())).col;
            Integer customColor = ClientConfig.getBlockColor(block);
            color = (customColor != null) ? customColor : (mapColor != 0 ? mapColor : DEFAULT_COLOR);

            FluidState fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
                Integer fluidColor = ClientConfig.getFluidColor(fluidState.getType());
                if (fluidColor != null) {
                    color = fluidColor;
                }
            }

            if (block instanceof SpawnerBlock && level instanceof Level realLevel) {
                for (BlockPos bp : blocks) {
                    if (realLevel.getBlockEntity(bp) instanceof SpawnerBlockEntity spawner) {
                        var display = spawner.getSpawner().getOrCreateDisplayEntity(realLevel, bp);
                        if (display != null) {
                            label = Component.translatable("gui.scannable_unofficial.overlay.spawner",
                                    display.getType().getDescription());
                            break;
                        }
                    }
                }
            }
        }

        boolean isRoot() { return parent == null; }

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

        @Nullable @Override public AABB getRenderBounds() { return bounds; }
        @Override public Vec3 getPosition() { return bounds.getCenter(); }
        @Override public void close() {}
    }
}
