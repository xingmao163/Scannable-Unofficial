package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.Scannable;
import com.starmao.scannable.client.ScanManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;

/**
 * Utility for rendering the player's hands into the depth buffer with colour
 * writes disabled, preventing scanner overlays from appearing on top of held
 * items in first-person view.
 */
public final class HandDepthRenderer {

    /**
     * Renders the player's hands into the depth buffer only (colour writes
     * disabled). Must be called from the render thread during
     * {@link net.neoforged.neoforge.client.event.RenderLevelStageEvent}.
     *
     * @param partialTicks current partial tick time
     */
    public static void writeHandDepth(final float partialTicks) {
        final Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()
                || mc.options.hideGui
                || mc.gameMode.getPlayerMode() == GameType.SPECTATOR
                || mc.player == null
                || (mc.getCameraEntity() instanceof LivingEntity living && living.isSleeping())) {
            return;
        }

        final PoseStack viewPose = ScanManager.getWorldViewModelStack();
        if (viewPose == null) return;

        RenderSystem.colorMask(false, false, false, false);
        try {
            final Matrix4f viewMat = new Matrix4f(viewPose.last().pose());
            final var mvStack = RenderSystem.getModelViewStack();
            mvStack.pushMatrix().mul(viewMat);

            final PoseStack handPose = new PoseStack();
            handPose.pushPose();
            handPose.mulPose(viewMat.invert(new Matrix4f()));

            final var bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(256));
            mc.gameRenderer.itemInHandRenderer.renderHandsWithItems(
                    partialTicks, handPose, bufferSource,
                    (LocalPlayer) mc.player,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partialTicks));
            bufferSource.endBatch();

            handPose.popPose();
            mvStack.popMatrix();
        } catch (final Throwable e) {
            Scannable.LOGGER.error("Failed to render hand into depth buffer", e);
        }
        RenderSystem.colorMask(true, true, true, true);
    }

    private HandDepthRenderer() {}
}
