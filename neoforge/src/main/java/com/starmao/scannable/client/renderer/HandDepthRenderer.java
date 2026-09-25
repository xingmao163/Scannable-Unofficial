package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.Scannable;
import com.starmao.scannable.client.ScanManager;
import com.starmao.scannable.client.config.ClientConfig;
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
 *
 * <p>Must be called from the render thread during
 * {@link net.neoforged.neoforge.client.event.RenderLevelStageEvent}.
 *
 * <p><strong>Failure containment.</strong> This pass re-enters the vanilla first-person
 * hand pipeline, so it can fail for reasons outside this mod's control — a third-party
 * mod taking over hand rendering, an unexpected item render type, and so on. Three
 * safeguards keep such a failure contained:
 * <ul>
 *   <li>the model-view matrix push is unwound in a {@code finally} block, so a failure can
 *       never leak an entry on {@code RenderSystem.getModelViewStack()}. That stack holds
 *       16 entries and is shared by the whole client, so leaking one entry per frame
 *       eventually crashed the game inside an <em>unrelated</em> mod's GUI layer;</li>
 *   <li>after {@value #MAX_FAILURES} consecutive failures the pass disables itself for the
 *       rest of the session instead of retrying (and logging) every single frame;</li>
 *   <li>it renders through a buffer source whose fixed table covers the glint render types,
 *       so drawing an enchanted item cannot trip {@code Not building!}
 *       (see {@link ScanRenderBuffers}).</li>
 * </ul>
 *
 * <p>The pass can be switched off entirely via {@code rendering.handDepthPass} in the
 * client config.
 */
public final class HandDepthRenderer {

    /** Consecutive failures after which the pass turns itself off for this session. */
    private static final int MAX_FAILURES = 3;

    /**
     * Long-lived buffer source. Reuse is safe (see {@link ScanRenderBuffers}) and mirrors
     * vanilla {@code RenderBuffers}, which also keeps its sources for the client's lifetime.
     */
    private static final MultiBufferSource.BufferSource BUFFER = ScanRenderBuffers.create();

    private static int consecutiveFailures;
    private static boolean disabled;

    /**
     * Renders the player's hands into the depth buffer only (colour writes
     * disabled). Must be called from the render thread during
     * {@link net.neoforged.neoforge.client.event.RenderLevelStageEvent}.
     *
     * @param partialTicks current partial tick time
     */
    public static void writeHandDepth(final float partialTicks) {
        if (disabled || !isEnabledInConfig()) return;

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

        final var mvStack = RenderSystem.getModelViewStack();
        boolean pushed = false;

        RenderSystem.colorMask(false, false, false, false);
        try {
            final Matrix4f viewMat = new Matrix4f(viewPose.last().pose());
            mvStack.pushMatrix().mul(viewMat);
            pushed = true;

            // handPose is a local stack used only by this call, so it needs no unwinding.
            final PoseStack handPose = new PoseStack();
            handPose.mulPose(viewMat.invert(new Matrix4f()));

            // renderHandsWithItems() flushes the buffer source itself as its last
            // statement (ItemInHandRenderer#renderHandsWithItems), so an extra
            // endBatch() here would be redundant.
            mc.gameRenderer.itemInHandRenderer.renderHandsWithItems(
                    partialTicks, handPose, BUFFER,
                    (LocalPlayer) mc.player,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partialTicks));

            consecutiveFailures = 0;
        } catch (final Throwable e) {
            if (++consecutiveFailures >= MAX_FAILURES) {
                disabled = true;
                Scannable.LOGGER.warn("Disabling the scanner hand-depth pass after {} consecutive failures; "
                        + "scan highlights may draw over the hand from now on", MAX_FAILURES, e);
            } else {
                Scannable.LOGGER.error("Failed to render hand into depth buffer", e);
            }
        } finally {
            if (pushed) mvStack.popMatrix();
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    /**
     * @return whether the pass is enabled in the client config; defaults to {@code true}
     *         when the config is not loaded yet
     */
    private static boolean isEnabledInConfig() {
        try {
            return ClientConfig.HAND_DEPTH_PASS.get();
        } catch (final IllegalStateException e) {
            return true;
        }
    }

    private HandDepthRenderer() {}
}
