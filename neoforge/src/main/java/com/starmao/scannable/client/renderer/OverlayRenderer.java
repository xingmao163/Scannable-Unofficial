package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
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
 * Scan progress indicator. Draws a textured circular sector matching the
 * 1.21.1 visual: a clock-wipe fan with the scanner_progress texture mapped
 * to the ring area. Each segment is a thin slice of the texture (1 px wide)
 * rotated around the screen centre.
 *
 * <p>Uses {@link GuiGraphicsExtractor#blit(RenderPipeline, Identifier, int, int, float, float, int, int, int, int, int, int, int)}
 * where the int params are (x, y, outputW, outputH, regionW, regionH, texW, texH, color)
 * and the float params are (uMin, vMin) in pixels.</p>
 */
public final class OverlayRenderer {
    private static final Identifier PROGRESS =
            Scannable.id("textures/gui/overlay/scanner_progress.png");
    private static final int SIZE = 64;
    private static final int INNER_R = 10;
    private static final int OUTER_R = 32;
    private static final int SEGMENTS = 60;
    private static final float SEG_ANGLE = (float) (Math.PI * 2 / SEGMENTS);
    private static final int FILL_COLOR = 0xA8A8CCED;
    private static final int BG_COLOR = 0x66000000;

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

        // -- Background ring: solid dark fill --
        // Each segment is a rotated rectangle using fill(x0, y0, x1, y1, color).
        // fill() sorts the coordinates so (x0,y0)-(x1,y1) can be any order.
        pose.pushMatrix();
        pose.translate(cx, cy);
        for (int i = 0; i < SEGMENTS; i++) {
            final float angle = i * SEG_ANGLE - (float) Math.PI / 2;
            pose.pushMatrix();
            pose.rotate(angle);
            graphics.fill(INNER_R, -1, OUTER_R, 1, BG_COLOR);
            pose.popMatrix();
        }
        pose.popMatrix();

        // -- Filled sector: textured wedges --
        // Uses the 13-param blit(pipeline, id, x, y, uMin, vMin,
        //                        outW, outH, regW, regH, texW, texH, color)
        // where uMin/vMin are in PIXEL coordinates (0..63).
        // Each segment maps a 1×64 pixel vertical strip of the texture
        // to a segW×2 rectangle, rotated around (cx,cy).
        final int filled = (int) (SEGMENTS * progress);
        pose.pushMatrix();
        pose.translate(cx, cy);
        for (int i = 0; i < filled; i++) {
            final float angle = i * SEG_ANGLE - (float) Math.PI / 2;
            pose.pushMatrix();
            pose.rotate(angle);
            graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS,
                    INNER_R, -1,            // x, y (output position)
                    (float) i, 0f,          // uMin, vMin (texture pixel origin)
                    segW, 2,                // output width, output height
                    1, SIZE,               // texture region width, height (1 × 64)
                    SIZE, SIZE,            // texture dimensions (64 × 64)
                    FILL_COLOR);            // tint colour (0xA8A8CCED)
            pose.popMatrix();
        }
        pose.popMatrix();

        // -- Percentage label --
        final Component label = Strings.progress(Mth.floor(progress * 100));
        graphics.text(mc.font, label, cx + OUTER_R + 12, cy - mc.font.lineHeight / 2, 0xCCAACCEE, true);
    }

    private OverlayRenderer() {}
}
