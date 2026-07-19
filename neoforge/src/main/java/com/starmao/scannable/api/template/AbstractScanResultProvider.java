package com.starmao.scannable.api.template;

import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Abstract base for scan result providers that detect block entities.
 * <p>Handles the common lifecycle: initialise with scan params, accumulate results
 * during {@link #computeScanResults} by iterating over a cubic bounding box,
 * collect results via a consumer, render bounding-box highlights with colour,
 * and reset on scan end.
 * <p>Subclasses define {@link #getBlockEntityClass()} and {@link #getColor}
 * to customise which block entities to detect and what colour to highlight them.
 * Implements {@link ResourceManagerReloadListener} for shader reload handling.
 *
 * @param <T> the block entity type this provider scans for
 */
public abstract class AbstractScanResultProvider<T extends BlockEntity> implements ScanResultProvider, ResourceManagerReloadListener {

    private Vec3 scanCenter;
    private float scanRadius;
    private Player scanPlayer;
    private List<T> foundData;

    private final Map<BlockEntity, ScanResultImpl> currentResults = new HashMap<>();

    protected AbstractScanResultProvider() {
    }

    /** @return the class of the block entity type this provider detects */
    protected abstract Class<T> getBlockEntityClass();

    /**
     * Returns the highlight ARGB colour for a given scan result.
     *
     * @param data the detected block entity
     * @return the colour as ARGB int (e.g. {@code 0xRRGGBB})
     */
    protected abstract int getColor(T data);

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        this.scanCenter = center;
        this.scanRadius = radius;
        this.scanPlayer = player;
        this.foundData = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void computeScanResults() {
        if (scanCenter == null || scanPlayer == null || scanRadius <= 0) return;

        BlockGetter level = scanPlayer.level();
        int radius = (int) Math.ceil(scanRadius);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    var pos = net.minecraft.core.BlockPos.containing(scanCenter).offset(dx, dy, dz);

                    if (scanCenter.distanceToSqr(Vec3.atCenterOf(pos)) > scanRadius * scanRadius) continue;

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null) continue;

                    // Check if this BE has already been found
                    if (currentResults.containsKey(be)) continue;

                    if (getBlockEntityClass().isInstance(be)) {
                        foundData.add((T) be);
                    }
                }
            }
        }
    }

    @Override
    public void collectScanResults(BlockGetter level, java.util.function.Consumer<ScanResult> callback) {
        if (foundData == null || foundData.isEmpty()) return;

        for (T data : foundData) {
            currentResults.computeIfAbsent((BlockEntity) data, be -> {
                AABB box = new AABB(be.getBlockPos());
                return new ScanResultImpl(Vec3.atCenterOf(be.getBlockPos()), box, getColor(data));
            });
        }

        currentResults.values().forEach(callback);
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                       Camera renderInfo, float partialTicks, List<ScanResult> results) {
        for (ScanResult result : results) {
            if (result instanceof ScanResultImpl impl) {
                renderBox(poseStack, bufferSource, impl);
            }
        }
    }

    private void renderBox(PoseStack poseStack, MultiBufferSource bufferSource, ScanResultImpl result) {
        if (result.renderBounds == null) return;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        int color = result.color;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        double minX = result.renderBounds.minX;
        double minY = result.renderBounds.minY;
        double minZ = result.renderBounds.minZ;
        double maxX = result.renderBounds.maxX;
        double maxY = result.renderBounds.maxY;
        double maxZ = result.renderBounds.maxZ;

        // Bottom face
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) minY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) minY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) minY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) minY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) minY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) minY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) minY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) minY, (float) minZ).setColor(r, g, b, 1.0f);

        // Top face
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) maxY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) maxY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) maxY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) maxY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) maxY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) maxY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) maxY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) maxY, (float) minZ).setColor(r, g, b, 1.0f);

        // Vertical edges
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) minY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) maxY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) minY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) maxY, (float) minZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) minY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) maxX, (float) maxY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) minY, (float) maxZ).setColor(r, g, b, 1.0f);
        vertexConsumer.addVertex(poseStack.last(), (float) minX, (float) maxY, (float) maxZ).setColor(r, g, b, 1.0f);
    }

    @Override
    public void reset() {
        foundData = null;
        currentResults.clear();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        reset();
    }

    // ---- Internal result ---- //

    private record ScanResultImpl(Vec3 position, AABB renderBounds, int color) implements ScanResult {
        @Override
        public Vec3 getPosition() {
            return position;
        }

        @Override
        public AABB getRenderBounds() {
            return renderBounds;
        }
    }
}
