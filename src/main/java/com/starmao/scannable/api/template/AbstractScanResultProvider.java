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

import com.starmao.scannable.util.UnitConversion;

/**
 * Abstract base for scan result providers on 26.1.2.
 * <p>
 * Provides helper methods for rendering using the new RenderPipeline / RenderType system.
 */
public abstract class AbstractScanResultProvider implements ScanResultProvider {
    /**
     * Cap on simultaneously drawn result icons.
     *
     * <p>Icons are no longer gated on the look direction (see {@link #renderIconLabel}), so a scan
     * that finds many results would otherwise plaster one icon per result over the screen. The cap
     * keeps the display readable and gives the limited slots to the best-aimed results.
     */
    protected static final int MAX_ICONS = 4;

    protected Player player;
    protected Vec3 center;
    protected int radius;

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        this.player = player;
        this.center = center;
        this.radius = (int) radius;
    }

    @Override
    public void reset() {
        this.player = null;
        this.center = null;
        this.radius = 0;
    }

    // ---- Icon labels (billboarded) -------------------------------------------

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

    /**
     * Renders a billboarded icon, plus the name label once the result is being looked at.
     *
     * <p>The two are gated differently, matching the 1.21.1 overlay: the icon is drawn for every
     * result the caller hands over, while the text label only appears when the crosshair is aimed
     * at the result ({@code lookDirDot > 0.999f}). Callers are responsible for any wider
     * visibility filtering; the icon must not be gated on the look direction here, or scanning
     * would appear to produce no results until the player aims at each one.
     *
     * <p>Callers are expected to pass a real icon texture — in practice every provider uses
     * {@code ModTextures.ICON_INFO} or a module-supplied icon. The {@code null} guard is defensive
     * only: it skips the icon quad rather than forwarding the value to
     * {@link ScanResultRenderType#icon}, which would bind a null texture and crash later, in
     * {@code endBatch()}, far from the offending call.
     */
    protected static void renderIconLabel(MultiBufferSource bufferSource, PoseStack poseStack,
                                           float yaw, float pitch, Vec3 lookVec, Vec3 viewerEyes,
                                           float displayDistance, Vec3 resultPos,
                                           @Nullable Identifier icon, @Nullable Component label) {
        final Vec3 toResult = resultPos.subtract(viewerEyes);
        final float distance = (float) toResult.length();
        final float lookDirDot = (float) lookVec.dot(toResult.normalize());
        final float sqLookDirDot = lookDirDot * lookDirDot;
        final float sq2LookDirDot = sqLookDirDot * sqLookDirDot;
        final float focusScale = Mth.clamp(sq2LookDirDot * sq2LookDirDot + 0.005f, 0.5f, 1f);
        final float scale = distance * focusScale * 0.005f;

        poseStack.pushPose();
        poseStack.translate(resultPos.x, resultPos.y, resultPos.z);
        poseStack.mulPose(new Quaternionf().rotationY(UnitConversion.toRadians(-yaw)));
        poseStack.mulPose(new Quaternionf().rotationX(UnitConversion.toRadians(pitch)));
        poseStack.scale(-scale, -scale, scale);

        if (lookDirDot > 0.999f && label != null) {
            final Component text = displayDistance > 0 ? withDistance(label, Mth.ceil(displayDistance)) : label;

            final Font font = Minecraft.getInstance().font;
            final int width = font.width(text) + 16;

            poseStack.pushPose();
            poseStack.translate(width / 2f, 0, 0);
            drawQuad(bufferSource.getBuffer(ScanResultRenderType.TYPE), poseStack, width, font.lineHeight + 5, 0, 0, 0, 0.6f);
            poseStack.popPose();

            font.drawInBatch(text, 12, -4, 0xFFFFFFFF, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0);
        }

        if (icon != null) {
            drawTexturedQuad(bufferSource.getBuffer(ScanResultRenderType.icon(icon)), poseStack, 16, 16);
        }

        poseStack.popPose();
    }

    // ---- Drawing primitives --------------------------------------------------

    /**
     * Draws a solid-colour quad centered at the current pose origin.
     */
    protected static void drawQuad(VertexConsumer buffer, PoseStack poseStack, float width, float height) {
        drawQuad(buffer, poseStack, width, height, 1, 1, 1, 1);
    }

    /**
     * Draws a solid-colour quad centered at the current pose origin
     * with the given tint colour.
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
     * Draws a textured quad centered at the current pose origin.
     */
    protected static void drawTexturedQuad(VertexConsumer buffer, PoseStack poseStack, float width, float height) {
        var pose = poseStack.last();
        var matrix = pose.pose();

        buffer.addVertex(matrix, -width * 0.5f, height * 0.5f, 0).setUv(0, 1).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix,  width * 0.5f, height * 0.5f, 0).setUv(1, 1).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix,  width * 0.5f, -height * 0.5f, 0).setUv(1, 0).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, -width * 0.5f, -height * 0.5f, 0).setUv(0, 0).setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Draws a solid-colour box bounded by the given AABB.
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
        // UVs are required, not decorative: the highlight is drawn with a POSITION_TEX_COLOR
        // pipeline, and BufferBuilder validates every vertex against that format on submit. A
        // vertex missing its UV aborts the frame with
        // "IllegalStateException: Missing elements in vertex: UV0" rather than rendering wrong.
        // One 0..1 span per face is enough — the shader uses the UV only as a geometric
        // coordinate for its edge glow, never to sample a texture.
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setUv(0, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x2, (float) y1, (float) z2).setUv(1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setUv(1, 1).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x1, (float) y2, (float) z1).setUv(0, 1).setColor(r, g, b, a);
    }

    private static Component withDistance(Component caption, float distance) {
        // Key must match the one defined in the lang files. It was previously missing the
        // "_unofficial.scanner" segment, so the raw key was drawn on screen instead of "%s (%sm)".
        return Component.translatable("gui.scannable_unofficial.scanner.overlay.distance", caption, Mth.ceil(distance));
    }
}
