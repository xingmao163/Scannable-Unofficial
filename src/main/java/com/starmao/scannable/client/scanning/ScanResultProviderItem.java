package com.starmao.scannable.client.scanning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Scan result provider for the container item scanner module.
 *
 * <p>Results are produced by a <em>server-side</em> scan and delivered over the
 * network, so {@link #computeScanResults()} is intentionally empty — there is
 * nothing to collect locally. {@link #createResult} is the entry point used by
 * {@code ScanManager#setServerItemResults}.
 */
public class ScanResultProviderItem extends AbstractScanResultProvider implements ScanResultProvider {

    /**
     * Kept for the {@code renderIconLabels} signature only.
     *
     * <p>Not a visibility control: {@code renderIconLabels} never reads its {@code minIconDot}
     * argument (it passes every result straight to {@code renderIconLabel}, which gates only the
     * text label on the crosshair), so changing this value has no effect. Named here so the dead
     * parameter is obvious rather than looking like a tunable.
     */
    private static final float UNUSED_ICON_CONE_DOT = 0.999f;

    private final List<ItemScanResult> results = new ArrayList<>();

    /** Converts one server-reported match into a renderable result. */
    public ItemScanResult createResult(final ItemScanResultData data) {
        final ItemStack stack = BuiltInRegistries.ITEM.getOptional(data.itemId())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        return new ItemScanResult(data.pos(), stack, data.totalCount());
    }

    @Override
    public void render(final ScanResultRenderContext context, final MultiBufferSource bufferSource,
                       final PoseStack poseStack, final Camera renderInfo, final float partialTicks,
                       final List<ScanResult> results) {
        if (results.isEmpty()) return;
        if (context == ScanResultRenderContext.WORLD) {
            renderWorldHighlights(bufferSource, poseStack, results);
        } else if (context == ScanResultRenderContext.GUI) {
            renderItemLabels(bufferSource, poseStack, renderInfo, results);
        }
    }

    /**
     * Not used: item results arrive from the server rather than being collected
     * locally. Present only to satisfy {@link ScanResultProvider}.
     */
    @Override
    public void computeScanResults() {}

    @Override
    public void collectScanResults(final BlockGetter level, final Consumer<ScanResult> callback) {
        results.forEach(callback::accept);
    }

    @Override
    public void reset() {
        super.reset();
        results.clear();
    }

    // ---- World highlights ---------------------------------------------------

    private void renderWorldHighlights(final MultiBufferSource bufferSource, final PoseStack poseStack,
                                       final List<ScanResult> results) {
        final VertexConsumer fill = bufferSource.getBuffer(ScanResultRenderType.SHIMMER_TYPE);
        for (final ScanResult result : results) {
            final ItemScanResult ir = (ItemScanResult) result;
            final int c = resolveColor(ir.pos);
            final float r = ((c >> 16) & 0xFF) / 255.0f;
            final float g = ((c >> 8) & 0xFF) / 255.0f;
            final float b = (c & 0xFF) / 255.0f;
            // Slightly inset so the highlight hugs the block rather than z-fighting it.
            drawBox(fill, poseStack,
                    ir.pos.getX() + 0.002, ir.pos.getY() + 0.002, ir.pos.getZ() + 0.002,
                    ir.pos.getX() + 0.998, ir.pos.getY() + 0.998, ir.pos.getZ() + 0.998,
                    ((int) (0.55f * 255) << 24) | ((int) (r * 255) << 16)
                            | ((int) (g * 255) << 8) | (int) (b * 255));
        }
    }

    /**
     * Fallback colour for blocks that report no map colour (map colour {@code 0}).
     * Matches {@code ScanResultProviderBlock#DEFAULT_COLOR} so item and block highlights
     * of the same container look identical.
     */
    private static final int DEFAULT_COLOR = 0x4466CC;

    /**
     * Picks the highlight colour for a container.
     *
     * <p>Takes the block's map colour at {@code pos}, falling back to the per-block
     * override from {@link ClientConfig#getBlockColor} when one is configured. This
     * mirrors the 1.21.1 behaviour; the earlier 26.1.2 port returned a hard-coded
     * white because the colour used to be applied through the old shader system.
     */
    private static int resolveColor(final BlockPos pos) {
        final Level level = Minecraft.getInstance().level;
        if (level == null) return DEFAULT_COLOR;
        final BlockState state = level.getBlockState(pos);
        final Integer override = ClientConfig.getBlockColor(state.getBlock());
        if (override != null) return override;
        // Blocks with no map colour report 0, which would tint the highlight fully
        // transparent and make it invisible — fall back like the block provider does.
        final int mapColor = state.getMapColor(level, pos).col;
        return mapColor != 0 ? mapColor : DEFAULT_COLOR;
    }

    // ---- GUI labels ---------------------------------------------------------

    private void renderItemLabels(final MultiBufferSource bufferSource, final PoseStack poseStack,
                                  final Camera camera, final List<ScanResult> results) {
        // Merge duplicate entries for the same container into one label listing
        // every matched item and its total count.
        //
        // Keyed by Identifier rather than ItemStack: ItemStack does not override
        // equals/hashCode, so a Map keyed by it compares by reference and would
        // never merge (each createResult allocates a fresh stack).
        final Map<BlockPos, Map<Identifier, ContainerItem>> byPos = new LinkedHashMap<>();
        for (final ScanResult result : results) {
            final ItemScanResult ir = (ItemScanResult) result;
            final Identifier id = BuiltInRegistries.ITEM.getKey(ir.stack.getItem());
            byPos.computeIfAbsent(ir.pos, k -> new LinkedHashMap<>())
                    .merge(id, new ContainerItem(ir.stack, ir.count),
                            (a, b) -> new ContainerItem(a.stack, a.count + b.count));
        }

        final List<ScanResult> deduped = new ArrayList<>();
        for (final Map.Entry<BlockPos, Map<Identifier, ContainerItem>> entry : byPos.entrySet()) {
            final Map<Identifier, ContainerItem> items = entry.getValue();
            if (items.isEmpty()) continue;
            final ContainerItem first = items.values().iterator().next();
            deduped.add(new ItemScanResult(entry.getKey(), first.stack, first.count));
        }

        final org.joml.Vector3fc forward = camera.forwardVector();
        final Vec3 lookVec = new Vec3(forward.x(), forward.y(), forward.z());
        final Vec3 viewerEyes = camera.position();
        final Player player = Minecraft.getInstance().player;
        final boolean showDistance = player != null && player.isShiftKeyDown();

        deduped.sort(Comparator.comparing(
                result -> lookVec.dot(result.getPosition().subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, camera.yRot(), camera.xRot(), lookVec, viewerEyes,
                showDistance, deduped,
                ScanResult::getPosition,
                result -> ModTextures.ICON_INFO,
                result -> {
                    final Map<Identifier, ContainerItem> items =
                            byPos.get(((ItemScanResult) result).pos);
                    if (items == null || items.isEmpty()) return Component.literal("");
                    final StringBuilder sb = new StringBuilder();
                    for (final ContainerItem item : items.values()) {
                        if (!sb.isEmpty()) sb.append(", ");
                        sb.append(item.stack.getHoverName().getString())
                                .append(" x").append(item.count);
                    }
                    return Component.literal(sb.toString());
                },
                result -> true,
                MAX_ICONS, UNUSED_ICON_CONE_DOT);
    }

    // ---- Result type --------------------------------------------------------

    /**
     * Aggregated match for one item type inside one container: the stack used
     * as the label's icon source plus the summed count.
     */
    private record ContainerItem(ItemStack stack, int count) {}

    /** A single matched container, holding the first matched item as its icon source. */
    public static final class ItemScanResult implements ScanResult {
        private final BlockPos pos;
        private final ItemStack stack;
        private final int count;

        ItemScanResult(final BlockPos pos, final ItemStack stack, final int count) {
            this.pos = pos;
            this.stack = stack;
            this.count = count;
        }

        @Override
        public Vec3 getPosition() {
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        @Override
        @Nullable
        public AABB getRenderBounds() {
            return new AABB(pos);
        }
    }
}
