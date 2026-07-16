package com.starmao.scannable.client.scanning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.starmao.scannable.Scannable;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Consumer;

/**
 * Scan result provider for the container item scanner module.
 *
 * <p>Scans all blocks within range and checks block entities for an item handler
 * capability ({@link IItemHandler}). For containers that have one, iterates their
 * inventory looking for user-configured items. Matching containers are highlighted
 * with the found item's name and total quantity displayed.
 */
public final class ScanResultProviderItem extends AbstractScanResultProvider {
    private static final float HIGHLIGHT_ALPHA = 0.8f;

    private static int getHighlightColor() {
        try {
            final String colorStr = ClientConfig.ITEM_SCAN_COLOR.get();
            if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                return Integer.parseUnsignedInt(colorStr.substring(2), 16);
            }
            return Integer.parseUnsignedInt(colorStr, 16);
        } catch (final Exception e) {
            return 0xBB44FF; // fallback purple
        }
    }

    private List<Item> targetItems = List.of();
    private final List<ItemScanResult> results = new ArrayList<>();

    // Multi-tick chunk scanning state
    private final List<ChunkSectionPos> pendingChunkSections = new ArrayList<>();
    private int currentChunkSection;
    private int chunkSectionsPerTick;

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);

        results.clear();
        pendingChunkSections.clear();
        targetItems = List.of();

        // Read configured target items from the item scanner module's stack
        for (ItemStack stack : modules) {
            if (stack.getItem() instanceof ConfigurableItemScannerModuleItem moduleItem) {
                targetItems = moduleItem.getValues(stack);
                break;
            }
        }

        if (targetItems.isEmpty()) {
            Scannable.LOGGER.warn("[ItemScanner] No target items configured!");
            if (player != null) {
                player.sendSystemMessage(
                        Component.translatable("message.scannable_unofficial.scanner.no_target_items"));
            }
            return;
        }

        Scannable.LOGGER.info("[ItemScanner] Scanning for {} target item(s):", targetItems.size());
        for (Item target : targetItems) {
            Scannable.LOGGER.info("[ItemScanner]   - {}", target.getName(target.getDefaultInstance()).getString());
        }

        // Calculate chunk sections that intersect the scan sphere
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

                        int gx = originX + x;
                        int gy = bottomY + y;
                        int gz = originZ + z;
                        BlockPos pos = new BlockPos(gx, gy, gz);

                        // Quick distance check
                        if (center.distanceToSqr(gx + 0.5, gy + 0.5, gz + 0.5) > this.radius * this.radius) {
                            continue;
                        }

                        // Note: Client-side item handler capability scanning was removed
                        // in NeoForge 26.1.2. Server-side item scan results are still
                        // injected via ScanManager.setServerItemResults().
                        if (true) continue;
                        // The following is a placeholder for the new container scanning API
                        // when it becomes available:
                        /*
                        // Scan inventory for target items -- track each item type separately
                        java.util.Map<Item, Integer> itemCounts = new java.util.HashMap<>();
                        for (int slot = 0; slot < 0; slot++) {
                            ItemStack slotStack = ItemStack.EMPTY;
                            if (slotStack.isEmpty()) continue;
                            if (isTargetItem(slotStack)) {
                                itemCounts.merge(slotStack.getItem(), slotStack.getCount(), Integer::sum);
                            }
                        }*/

                        /* // Temporarily disabled (no IItemHandler in 26.1.2)
                        for (var entry : itemCounts.entrySet()) {
                            results.add(new ItemScanResult(pos, entry.getKey().getDefaultInstance(), entry.getValue(), 0xBB44FF));
                            Scannable.LOGGER.info("[ItemScanner] Found {} x{} at {}",
                                    entry.getKey().getDefaultInstance().getHoverName().getString(), entry.getValue(), pos);
                        }*/
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
        Scannable.LOGGER.info("[ItemScanner] Collecting {} scan result(s)", results.size());
        for (ItemScanResult result : results) {
            BlockState blockState = level.getBlockState(result.pos());
            int color = blockState.getMapColor(level, result.pos()).col;
            if (color == 0) color = 0x4466CC;  // DEFAULT_COLOR from BlockScanResult

            ItemScanResult enrichedResult = new ItemScanResult(
                    result.pos(),
                    result.item(),
                    result.totalCount(),
                    color
            );
            callback.accept(enrichedResult);
        }
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                       Camera renderInfo, float partialTicks, List<ScanResult> results) {
        if (results.isEmpty()) return;

        switch (context) {
            case WORLD -> renderHighlights(bufferSource, poseStack, renderInfo, results);
            case GUI -> renderItemLabels(bufferSource, poseStack, renderInfo, results);
        }
    }

    @Override
    public void reset() {
        super.reset();
        targetItems = List.of();
        results.clear();
        pendingChunkSections.clear();
        currentChunkSection = 0;
        chunkSectionsPerTick = 0;
    }

    // ====================================================================
    // Rendering -- World (block highlights)
    // ====================================================================

    private void renderHighlights(MultiBufferSource bufferSource, PoseStack poseStack, Camera camera,
                                  List<ScanResult> results) {
        final var matrix = poseStack.last().pose();

        // Fill faces using the scan-result pipeline (POSITION_COLOR)
        final VertexConsumer fill = bufferSource.getBuffer(ScanResultRenderType.TYPE);

        // Outline edges using the lines pipeline (POSITION_COLOR)
        final VertexConsumer outline = bufferSource.getBuffer(ScanResultRenderType.LINES_TYPE);

        Level level = Minecraft.getInstance().level;
        for (ScanResult result : results) {
            ItemScanResult itemResult = (ItemScanResult) result;
            int color = itemResult.blockColor();
            // Re-fetch latest block color (in case the block changed)
            if (level != null) {
                BlockState blockState = level.getBlockState(itemResult.pos());
                int mapColor = blockState.getMapColor(level, itemResult.pos()).col;
                if (mapColor != 0) color = mapColor;
            }
            renderBoxFaces(fill, matrix, itemResult.pos(), color);
            renderBoxEdges(outline, matrix, itemResult.pos(), color);
        }
    }

    private static void renderBoxFaces(VertexConsumer buffer, Matrix4f matrix, BlockPos pos, int color) {
        float cx = pos.getX(), cy = pos.getY(), cz = pos.getZ();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // -X (0.8f)
        buffer.addVertex(matrix, cx, cy, cz).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, cx, cy, cz + 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, cx, cy + 1, cz + 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, cx, cy + 1, cz).setColor(r, g, b, 0.8f);
        // +X (0.8f)
        buffer.addVertex(matrix, cx + 1, cy, cz).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz + 1).setColor(r, g, b, 0.8f);
        buffer.addVertex(matrix, cx + 1, cy, cz + 1).setColor(r, g, b, 0.8f);
        // -Y (0.7f)
        buffer.addVertex(matrix, cx, cy, cz).setColor(r, g, b, 0.7f);
        buffer.addVertex(matrix, cx + 1, cy, cz).setColor(r, g, b, 0.7f);
        buffer.addVertex(matrix, cx + 1, cy, cz + 1).setColor(r, g, b, 0.7f);
        buffer.addVertex(matrix, cx, cy, cz + 1).setColor(r, g, b, 0.7f);
        // +Y (1.0f)
        buffer.addVertex(matrix, cx, cy + 1, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz).setColor(r, g, b, 1.0f);
        // -Z (0.9f)
        buffer.addVertex(matrix, cx, cy, cz).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, cx, cy + 1, cz).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, cx + 1, cy, cz).setColor(r, g, b, 0.9f);
        // +Z (0.9f)
        buffer.addVertex(matrix, cx, cy, cz + 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, cx + 1, cy, cz + 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz + 1).setColor(r, g, b, 0.9f);
        buffer.addVertex(matrix, cx, cy + 1, cz + 1).setColor(r, g, b, 0.9f);
    }

    private static void renderBoxEdges(VertexConsumer buffer, Matrix4f matrix, BlockPos pos, int color) {
        float cx = pos.getX(), cy = pos.getY(), cz = pos.getZ();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // Bottom face edges
        buffer.addVertex(matrix, cx, cy, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy, cz).setColor(r, g, b, 1.0f);
        // Top face edges
        buffer.addVertex(matrix, cx, cy + 1, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy + 1, cz).setColor(r, g, b, 1.0f);
        // Vertical edges
        buffer.addVertex(matrix, cx, cy, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy + 1, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx + 1, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy, cz + 1).setColor(r, g, b, 1.0f);
        buffer.addVertex(matrix, cx, cy + 1, cz + 1).setColor(r, g, b, 1.0f);
    }

    // ====================================================================
    // Rendering -- GUI (item labels in world view)
    // ====================================================================

    private void renderItemLabels(MultiBufferSource bufferSource, PoseStack poseStack, Camera renderInfo,
                                  List<ScanResult> results) {
        Vec3 lookVec = new Vec3(renderInfo.forwardVector());
        Vec3 viewerEyes = renderInfo.position();
        float yaw = renderInfo.yRot();
        float pitch = renderInfo.xRot();
        boolean showDistance = renderInfo.entity().isShiftKeyDown();

        // Group results by position -- same container may have multiple matching item types
        final java.util.Map<BlockPos, java.util.List<ItemScanResult>> byPos = new java.util.LinkedHashMap<>();
        for (final ScanResult r : results) {
            final ItemScanResult ir = (ItemScanResult) r;
            byPos.computeIfAbsent(ir.pos(), k -> new java.util.ArrayList<>()).add(ir);
        }

        // Keep only first result per position -- we'll show all items in the label
        final java.util.List<ScanResult> deduped = new java.util.ArrayList<>();
        for (final var entry : byPos.entrySet()) {
            deduped.add(entry.getValue().get(0));
        }

        deduped.sort(Comparator.comparing(r ->
                lookVec.dot(r.getPosition().subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes, showDistance, deduped,
                ScanResult::getPosition,
                result -> ModTextures.ICON_INFO,
                result -> {
                    // Show ALL configured items found in this container
                    final java.util.List<ItemScanResult> allItems = byPos.get(
                            ((ItemScanResult) result).pos());
                    if (allItems == null || allItems.size() == 1) {
                        ItemScanResult r = (ItemScanResult) result;
                        return Component.literal(r.item().getHoverName().getString())
                                .append(Component.literal(" x" + r.totalCount()));
                    }
                    final StringBuilder sb = new StringBuilder();
                    for (final ItemScanResult ir : allItems) {
                        if (!sb.isEmpty()) sb.append(", ");
                        sb.append(ir.item().getHoverName().getString())
                          .append(" x").append(ir.totalCount());
                    }
                    return Component.literal(sb.toString());
                },
                result -> true,
                MAX_ICONS, ICON_CONE_DOT);
    }

    // ====================================================================
    // Data classes
    // ====================================================================

    private record ChunkSectionPos(int chunkX, int chunkZ, int chunkSectionIndex, double squareDistToCenter) {}

}
