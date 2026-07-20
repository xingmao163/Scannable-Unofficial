package com.starmao.scannable.api.template;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.function.Predicate;

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
        this.radius = (int) radius;
    }

    @Override
    public void reset() {
        player = null;
        center = null;
        radius = 0;
    }

    protected static <T> void renderIconLabels(MultiBufferSource bufferSource, PoseStack poseStack,
                                                float yaw, float pitch, Vec3 lookVec, Vec3 viewerEyes,
                                                boolean showDistance, List<T> results,
                                                Function<T, Vec3> position, Function<T, Identifier> icon,
                                                Function<T, Component> name, Predicate<T> visible,
                                                int maxIcons, float minIconDot) {
        int shown = 0;
        boolean nameShown = false;
        for (int i = results.size() - 1; i >= 0 && shown < maxIcons; i--) {
            T result = results.get(i);
            Vec3 resultPos = position.apply(result);
            Vec3 toResult = resultPos.subtract(viewerEyes);
            float lookDirDot = (float) lookVec.dot(toResult.normalize());
            if (lookDirDot <= minIconDot) break;
            if (!visible.test(result)) continue;


            Component label = null;
            Component candidate = name.apply(result);
            if (candidate != null && !candidate.getString().isEmpty()) {
                label = candidate;
            }

            float distance = showDistance ? (float) toResult.length() : 0f;
            renderIconLabel(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes,
                    distance, resultPos, icon.apply(result), label);
            shown++;
        }
    }

    protected static void renderIconLabel(MultiBufferSource bufferSource, PoseStack poseStack,
                                           float yaw, float pitch, Vec3 lookVec, Vec3 viewerEyes,
                                           float displayDistance, Vec3 resultPos,
                                           Identifier icon, @Nullable Component label) {
        Vec3 toResult = resultPos.subtract(viewerEyes);
        float distance = (float) toResult.length();
        float lookDirDot = (float) lookVec.dot(toResult.normalize());
        float sqLookDirDot = lookDirDot * lookDirDot;
        float sq2LookDirDot = sqLookDirDot * sqLookDirDot;
        float focusScale = Mth.clamp(sq2LookDirDot * sq2LookDirDot + 0.005f, 0.5f, 1f);
        float scale = distance * focusScale * 0.005f;

        poseStack.pushPose();
        poseStack.translate(resultPos.x, resultPos.y, resultPos.z);
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-yaw)));
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(pitch)));
        poseStack.scale(-scale, -scale, scale);

        if (lookDirDot > 0.999f && label != null) {
            Component text = displayDistance > 0
                    ? Component.translatable("gui.scannable_unofficial.scanner.overlay.distance", label, Mth.ceil(displayDistance))
                    : label;

            Font font = Minecraft.getInstance().font;
            int width = font.width(text) + 16;

            poseStack.pushPose();
            poseStack.translate(width / 2f, 0, 0);
            drawQuad(bufferSource.getBuffer(ScanResultRenderType.TYPE), poseStack, width, font.lineHeight + 5, 0, 0, 0, 0.6f);
            poseStack.popPose();

            font.drawInBatch(text, 12, -4, 0xFFFFFFFF, true, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0);
            font.drawInBatch(text, 12, -4, 0xFFFFFFFF, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0);
        }

        drawQuad(bufferSource.getBuffer(ScanResultRenderType.icon(icon)), poseStack, 16, 16);
        poseStack.popPose();
    }

    // ---- Drawing primitives ---- //

    protected static void drawQuad(VertexConsumer buffer, PoseStack poseStack, float width, float height) {
        drawQuad(buffer, poseStack, width, height, 1, 1, 1, 1);
    }

    protected static void drawQuad(VertexConsumer buffer, PoseStack poseStack, float width, float height,
                                    float r, float g, float b, float a) {
        var matrix = poseStack.last().pose();
        buffer.addVertex(matrix, -width * 0.5f, height * 0.5f, 0).setColor(r, g, b, a).setUv(0, 1f);
        buffer.addVertex(matrix, width * 0.5f, height * 0.5f, 0).setColor(r, g, b, a).setUv(1f, 1f);
        buffer.addVertex(matrix, width * 0.5f, -height * 0.5f, 0).setColor(r, g, b, a).setUv(1f, 0);
        buffer.addVertex(matrix, -width * 0.5f, -height * 0.5f, 0).setColor(r, g, b, a).setUv(0, 0);
    }

    // ---- Render layers ---- //

    protected static RenderType getRenderLayer() {
        return ScanResultRenderType.TYPE;
    }

    protected static RenderType getRenderLayer(Identifier textureLocation) {
        return ScanResultRenderType.icon(textureLocation);
    }
}
