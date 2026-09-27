package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.pipeline.ColorTargetState;

/**
 * Writes the player's hand into the depth buffer so scanner overlays are occluded by
 * the held item in first-person view.
 *
 * <p><b>Status: not implemented on 26.1.2.</b> The 1.21.1 implementation cannot be ported
 * directly, because the 26.1 render pipeline removed every API it relied on:
 *
 * <ul>
 *   <li>{@code RenderSystem.colorMask(...)} — removed. Colour writes are now controlled per
 *       pipeline via {@link ColorTargetState#WRITE_NONE}, so a pass can only be made depth-only
 *       by drawing with a dedicated pipeline.</li>
 *   <li>{@code ItemInHandRenderer#renderHandsWithItems} — still exists, but now takes a
 *       {@code SubmitNodeCollector} instead of a {@code MultiBufferSource}, and
 *       {@code GameRenderer#renderItemInHand} is private. Vanilla submits the hand model with
 *       its own render types, so there is no hook that lets a mod re-submit it depth-only.</li>
 *   <li>{@code RenderType} — construction is no longer public and exposes no pipeline override,
 *       so the hand's render types cannot be swapped for depth-only variants from outside.</li>
 * </ul>
 *
 * <p>Reimplementing this would mean re-submitting the hand geometry through the engine's
 * internal submit flow with {@code ColorTargetState.WRITE_NONE}. Until that is done, scan
 * highlights are <em>not</em> occluded by first-person held items on this version, and the
 * {@code rendering.handDepthPass} client option no longer exists.
 *
 * @see ScanRenderBuffers the matching 1.21.1-only helper, likewise a no-op here
 */
public final class HandDepthRenderer {

    /**
     * No-op on 26.1.2 — see the class documentation. Kept so the world-render hook, which is
     * shared with the 1.21.1 branch, still compiles and can keep calling it unconditionally.
     *
     * @param partialTicks current partial tick time, unused
     */
    public static void writeHandDepth(final float partialTicks) {
        // Intentionally empty. See the class javadoc for why this cannot be ported.
    }

    private HandDepthRenderer() {}
}
