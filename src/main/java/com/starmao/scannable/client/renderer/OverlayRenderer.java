package com.starmao.scannable.client.renderer;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.config.Strings;
import com.starmao.scannable.common.item.ScannerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Scan progress indicator, matching the 1.21.1 overlay: a textured ring that fills
 * clockwise from 12 o'clock while the scanner charges, with no background track.
 *
 * <p>The source texture {@code scanner_progress.png} is a 256x256 image containing an
 * annulus centred on the image. Because the GUI layer draws it one segment at a time, the
 * code samples the annulus's radial cross section and stretches that onto the on-screen
 * {@code INNER_R}..{@code OUTER_R} span, so the ring comes out at its intended thickness
 * rather than as a thin band around the middle of that span.
 *
 * <p><b>Why the segment count is high.</b> The GUI layer only exposes
 * {@link GuiGraphicsExtractor}, which can emit axis-aligned quads (and quads transformed
 * by the current {@linkplain GuiGraphicsExtractor#pose() pose} matrix) — there is no
 * {@code VertexConsumer} access to build one continuous triangle fan the way the 1.21.1
 * implementation does with {@code Tesselator}. The ring is therefore approximated by
 * {@link #SEGMENTS} rotated strips. At the previous count of 60 the individual strips were
 * visible as jagged stair-stepping, especially at the ring's inner and outer edges; at
 * {@link #SEGMENTS} the angular step is under a degree, so the edges read as smooth while
 * the whole ring still batches into one draw call.
 *
 * <p>Adjacent strips share their angular boundary, so there are no gaps between them.
 */
public final class OverlayRenderer {
    private static final Identifier PROGRESS =
            Scannable.id("textures/gui/overlay/scanner_progress.png");

    /** Hollow centre of the drawn ring, in GUI units. Matches the texture's inner radius. */
    private static final int INNER_R = 10;
    /** Outer edge of the drawn ring, in GUI units. Matches the texture's outer radius. */
    private static final int OUTER_R = 32;

    /**
     * Number of rotated strips used to approximate the ring. One degree of arc per strip
     * is enough to make the radial edges look continuous at GUI scale.
     */
    private static final int SEGMENTS = 360;

    /** Source texture is 256x256; UV coordinates below are given in its pixels. */
    private static final int TEX_SIZE = 256;

    /**
     * Texture coordinates of the ring band, in pixels, measured from
     * {@code scanner_progress.png}.
     *
     * <p>The texture is an annulus centred on ({@value #TEX_SIZE}/2, {@value #TEX_SIZE}/2).
     * Along the horizontal centre line the opaque band occupies the two radius ranges on
     * either side of the hollow middle; the left-hand one begins at x = {@value #RING_U0} and
     * is {@value #RING_BAND_W} px wide. That x range is the ring's <em>radial</em> cross
     * section, and it is what gets mapped onto the on-screen {@code INNER_R}..{@code OUTER_R}
     * span — sampling the whole 0..256 width instead would squeeze the band into a thin ring.
     */
    private static final int RING_U0 = 4;
    /** Width of the band's radial cross section, in texture pixels. */
    private static final int RING_BAND_W = 44;
    /**
     * Texture rows on which the radial cross section above is opaque along its whole width.
     * Measured from the texture: on the centre row itself the annulus's inner edge cuts into
     * x = {@value #RING_U0}..{@value #RING_U0} + {@value #RING_BAND_W}, so the fully opaque run
     * sits just off centre. Sampling outside it would read transparent pixels and leave gaps
     * in the ring.
     */
    private static final int RING_V0 = 134;
    private static final int RING_V1 = 151;

    private static final float SEG_ANGLE = (float) (Math.PI * 2 / SEGMENTS);
    private static final int FILL_COLOR = 0xA8A8CCED;

    public static void render(final GuiGraphicsExtractor graphics, final float partialTick) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (player == null) return;

        final ItemStack stack = player.getUseItem();
        if (stack.isEmpty() || !ScannerItem.isScanner(stack)) return;

        final int total = stack.getUseDuration(player);
        if (total <= 0) return;
        final int remaining = player.getUseItemRemainingTicks();
        final float progress = Mth.clamp(1 - (remaining - partialTick) / (float) total, 0, 1);
        if (progress <= 0) return;

        final int cx = graphics.guiWidth() / 2;
        final int cy = graphics.guiHeight() / 2;
        final int segW = OUTER_R - INNER_R;

        final var pose = graphics.pose();

        // -- Filled sector: textured ring --
        // A small patch of the annulus is sampled and mapped onto the INNER_R..OUTER_R radial
        // span, then rotated into place. In texture space the patch spans
        // u = RING_U0 .. RING_U0 + RING_BAND_W (the band's radial cross section, which becomes
        // the on-screen radial direction) and one row within RING_V0..RING_V1 (which becomes the
        // on-screen tangential direction).
        //
        // Sampling only the band's cross section — rather than the texture's full 0..256 width —
        // is what gives the ring its 1.21.1 thickness: the band then fills the whole radial span
        // instead of being squeezed into a thin ring around the middle of it.
        //
        // The tangential coordinate walks along the opaque run by the segment's own fraction of
        // the circle, so successive segments sample different pixels instead of all reading the
        // same row. Segment 0 starts at 12 o'clock and fills clockwise. No background track is
        // drawn.
        final int filled = Mth.clamp((int) (SEGMENTS * progress), 0, SEGMENTS);
        if (filled > 0) {
            // Tangential walk range: the run of centre-line rows where the radial cross section
            // is opaque across its whole width. Successive segments step along this run so they
            // sample different pixels rather than all reading the same row.
            final int tangentSpan = RING_V1 - RING_V0;
            pose.pushMatrix();
            pose.translate(cx, cy);
            for (int i = 0; i < filled; i++) {
                final float angle = i * SEG_ANGLE - (float) Math.PI / 2;
                final float tangent = RING_V0 + (float) i / SEGMENTS * tangentSpan;
                pose.pushMatrix();
                pose.rotate(angle);
                graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS,
                        INNER_R, -1,               // x, y (output position)
                        RING_U0, tangent,          // uMin, vMin (texture pixel origin)
                        segW, 2,                   // output width, output height
                        RING_BAND_W, 1,            // sampled region: radial span x 1px strip
                        TEX_SIZE, TEX_SIZE,        // texture dimensions (256 x 256)
                        FILL_COLOR);               // tint colour
                pose.popMatrix();
            }
            pose.popMatrix();
        }

        // -- Percentage label --
        final Component label = Strings.progress(Mth.floor(progress * 100));
        graphics.text(mc.font, label, cx + OUTER_R + 12, cy - mc.font.lineHeight / 2, 0xCCAACCEE, true);
    }

    private OverlayRenderer() {}
}
