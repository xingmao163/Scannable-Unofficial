package com.starmao.scannable.api.template;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Abstract base for scan result providers on 26.1.2.
 * <p>
 * Provides helper methods for rendering using the new RenderPipeline / RenderType system.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractScanResultProvider implements ScanResultProvider {
    protected Player player;
    protected Vec3 center;
    protected int radius;

    protected static final int MAX_ICONS = 4;
    protected static final float ICON_CONE_DOT = 0.999f;

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        this.player = player;
        this.center = center;
        this.radius = (int) Math.ceil(radius);
    }

    @Override
    public void reset() {
        this.player = null;
        this.center = null;
    }

    // -- Icon labels (billboarded) -------------------------------------------

    protected static <T> void renderIconLabels(MultiBufferSource bufferSource, PoseStack poseStack,
                                                float yaw, float pitch, Vec3 lookVec, Vec3 viewerEyes,
                                                boolean showDistance, List<T> results,
                                                Function<T, Vec3> position, Function<T, Identifier> icon,
                                                Function<T, Component> name, Predicate<T> visible,
                                                int maxIcons, float minIconDot) {
        for (T result : results) {
            if (maxIcons-- <= 0) break;
            Vec3 pos = position.apply(result);
            if (pos == null || !visible.test(result)) continue;
            renderIconLabel(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes,
                    (float) viewerEyes.distanceTo(pos), pos, icon.apply(result), name.apply(result));
        }
    }

    protected static void renderIconLabel(MultiBufferSource bufferSource, PoseStack poseStack,
                                           float yaw, float pitch, Vec3 lookVec, Vec3 viewerEyes,
                                           float displayDistance, Vec3 resultPos,
                                           Identifier icon, @Nullable Component label) {
        // Billboarding + label rendering using the icon pipeline.
        // Can be extended with 26.1.2 specific billboard rendering when needed.
    }

    // -- Drawing primitives --------------------------------------------------

    /**
     * Draws a filled, textured quad centered at the current pose origin.
     * Uses the shimmer pipeline for additive blending with the scan_result shader.
     */
    protected static void drawQuad(VertexConsumer buffer, PoseStack poseStack, float width, float height) {
        drawQuad(buffer, poseStack, width, height, 1, 1, 1, 1);
    }

    /**
     * Draws a filled, textured quad centered at the current pose origin
     * with the given tint colour. Uses the shimmer pipeline.
     */
    protected static void drawQuad(VertexConsumer buffer, PoseStack poseStack, float width, float height,
                                    float r, float g, float b, float a) {
        var pose = poseStack.last();
        var matrix = pose.pose();
        var halfW = width / 2;
        var halfH = height / 2;

        buffer.addVertex(matrix, -halfW, -halfH, 0).setColor((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255)).setUv(0, 0);
        buffer.addVertex(matrix, -halfW,  halfH, 0).setColor((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255)).setUv(0, 1);
        buffer.addVertex(matrix,  halfW,  halfH, 0).setColor((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255)).setUv(1, 1);
        buffer.addVertex(matrix,  halfW, -halfH, 0).setColor((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255)).setUv(1, 0);
    }

    /**
     * Draws a solid-colour box bounded by the given AABB.
     * Uses the result-box pipeline (POSITION_COLOR, through-wall, translucent).
     */
    protected static void drawBox(VertexConsumer buffer, PoseStack poseStack,
                                   double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ,
                                   int color) {
        var pose = poseStack.last();
        var matrix = pose.pose();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        // Bottom
        drawBoxFace(buffer, matrix, minX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        // Top
        drawBoxFace(buffer, matrix, minX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        // Front
        drawBoxFace(buffer, matrix, minX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        // Back
        drawBoxFace(buffer, matrix, minX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        // Left
        drawBoxFace(buffer, matrix, minX, minY, minZ, minX, maxY, maxZ, r, g, b, a);
        // Right
        drawBoxFace(buffer, matrix, maxX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void drawBoxFace(VertexConsumer buffer, org.joml.Matrix4fc matrix,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     int r, int g, int b, int a) {
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x2, (float) y1, (float) z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x1, (float) y2, (float) z1).setColor(r, g, b, a);
    }
}
