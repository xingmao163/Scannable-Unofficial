package com.starmao.scannable.client.renderer;

import com.starmao.scannable.client.config.ClientConfig;

/**
 * Writes the player's hand into the depth buffer so scanner overlays are occluded by
 * the held item in first-person view.
 *
 * <p><b>Status: not implemented on 26.1.2.</b> The 1.21.1 implementation cannot be ported
 * directly, because the 26.1 render pipeline removed or reshaped the mechanism it relied on:
 *
 * <ul>
 *   <li>{@code RenderSystem.colorMask(...)} — genuinely removed from
 *       {@code com.mojang.blaze3d.systems.RenderSystem} (verified against the 26.1.2
 *       artifact). Colour writes are now a per-pipeline property, so a pass can only be
 *       made depth-only by drawing through a pipeline whose colour target carries
 *       {@link com.mojang.blaze3d.pipeline.ColorTargetState#WRITE_NONE}.</li>
 *   <li>{@code GameRenderer#renderItemInHand} is {@code private}, and
 *       {@code ItemInHandRenderer#renderHandsWithItems} now takes a
 *       {@code SubmitNodeCollector} (a deferred submit queue) instead of the
 *       {@code MultiBufferSource} that 1.21.1 passed in. There is no public hook that
 *       lets a mod re-run vanilla's hand rendering synchronously, depth-only.</li>
 *   <li>Ordering compounds this: the mod's overlay is drawn from
 *       {@code RenderLevelStageEvent.AfterLevel}, which NeoForge fires <em>before</em>
 *       {@code GameRenderer} pushes the {@code "hand"} profiler section — and that section
 *       begins by calling {@code clearDepthTexture(...)} on the main render target. A depth
 *       pre-pass written at the 1.21.1 call site would therefore be wiped before the hand
 *       was ever drawn.</li>
 * </ul>
 *
 * <p>Note that {@code RenderType.create(String, RenderSetup)} <em>is</em> public on 26.1.2
 * (this mod's own {@link ScanResultRenderType} builds render types with it), so swapping in
 * a depth-only render type is not the obstacle. The obstacle is reconstructing the hand's
 * geometry and pose: a real fix means rebuilding it from
 * {@code EntityRenderState}/{@code ItemStackRenderState} and submitting through
 * {@code RenderPipelines.ITEM_SNIPPET} / {@code ENTITY_SNIPPET} with a {@code WRITE_NONE}
 * colour target, i.e. reimplementing vanilla's entire first-person hand transform chain.
 * Until that is done, scan highlights are <em>not</em> occluded by first-person held items
 * on this version.
 *
 * <p>The {@code rendering.handDepthPass} client option is still defined (see
 * {@link ClientConfig#HAND_DEPTH_PASS}) and is still read here via
 * {@link #isEnabledInConfig()}, so a copied config keeps its setting and the value is
 * honoured the moment the pass is implemented. It has no visible effect today.
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
        // Intentionally empty. The config gate is still evaluated so the option stays wired
        // to real code rather than being decorative, but there is no pass to enable yet.
        if (!isEnabledInConfig()) return;
    }

    /**
     * @return whether the pass is enabled in the client config; defaults to {@code true}
     *         when the config is not loaded yet. Mirrors the 1.21.1 helper of the same
     *         name so the two branches stay in step once the pass is restored.
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
