package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;

/**
 * Shared block face rendering geometry for scanner overlays.
 *
 * <p>Both {@code ScanResultProviderBlock.BlockScanResult} and
 * {@code ScanResultProviderItem} render six-sided block highlights with
 * identical vertex order and per-face alpha values. This class extracts
 * the geometry data so the two providers do not duplicate vertex ordering.
 */
public final class BlockFaceRenderer {

    /** Per-face alpha values: -X, +X, -Y, +Y, -Z, +Z. */
    private static final float[] FACE_ALPHAS = {0.8f, 0.8f, 0.7f, 1.0f, 0.9f, 0.9f};

    @FunctionalInterface
    public interface VertexEmitter {
        void emit(float x, float y, float z, float u, float v, float alpha);
    }

    /**
     * Emits the 24 vertices (6 faces × 4 vertices) for a block at the given position.
     * <p>Each face yields four vertices via {@link VertexEmitter#emit},
     * with per-face alpha matching the conventions in
     * {@code ScanResultProviderBlock.BlockScanResult.render()}.
     * UV coordinates are 0..1 per face.
     */
    public static void emitBlockFaces(BlockPos pos, VertexEmitter out) {
        float cx = pos.getX(), cy = pos.getY(), cz = pos.getZ();

        // -X face (alpha 0.8)
        out.emit(cx, cy, cz, 0, 0, FACE_ALPHAS[0]);
        out.emit(cx, cy, cz + 1, 0, 1, FACE_ALPHAS[0]);
        out.emit(cx, cy + 1, cz + 1, 1, 1, FACE_ALPHAS[0]);
        out.emit(cx, cy + 1, cz, 1, 0, FACE_ALPHAS[0]);
        // +X face (alpha 0.8)
        out.emit(cx + 1, cy, cz, 0, 0, FACE_ALPHAS[1]);
        out.emit(cx + 1, cy + 1, cz, 1, 0, FACE_ALPHAS[1]);
        out.emit(cx + 1, cy + 1, cz + 1, 1, 1, FACE_ALPHAS[1]);
        out.emit(cx + 1, cy, cz + 1, 0, 1, FACE_ALPHAS[1]);
        // -Y face (alpha 0.7)
        out.emit(cx, cy, cz, 0, 0, FACE_ALPHAS[2]);
        out.emit(cx + 1, cy, cz, 1, 0, FACE_ALPHAS[2]);
        out.emit(cx + 1, cy, cz + 1, 1, 1, FACE_ALPHAS[2]);
        out.emit(cx, cy, cz + 1, 0, 1, FACE_ALPHAS[2]);
        // +Y face (alpha 1.0)
        out.emit(cx, cy + 1, cz, 0, 0, FACE_ALPHAS[3]);
        out.emit(cx, cy + 1, cz + 1, 0, 1, FACE_ALPHAS[3]);
        out.emit(cx + 1, cy + 1, cz + 1, 1, 1, FACE_ALPHAS[3]);
        out.emit(cx + 1, cy + 1, cz, 1, 0, FACE_ALPHAS[3]);
        // -Z face (alpha 0.9)
        out.emit(cx, cy, cz, 0, 0, FACE_ALPHAS[4]);
        out.emit(cx, cy + 1, cz, 0, 1, FACE_ALPHAS[4]);
        out.emit(cx + 1, cy + 1, cz, 1, 1, FACE_ALPHAS[4]);
        out.emit(cx + 1, cy, cz, 1, 0, FACE_ALPHAS[4]);
        // +Z face (alpha 0.9)
        out.emit(cx, cy, cz + 1, 0, 0, FACE_ALPHAS[5]);
        out.emit(cx + 1, cy, cz + 1, 1, 0, FACE_ALPHAS[5]);
        out.emit(cx + 1, cy + 1, cz + 1, 1, 1, FACE_ALPHAS[5]);
        out.emit(cx, cy + 1, cz + 1, 0, 1, FACE_ALPHAS[5]);
    }

    private BlockFaceRenderer() {}
}
