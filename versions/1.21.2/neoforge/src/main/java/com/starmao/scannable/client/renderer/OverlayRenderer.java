package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starmao.scannable.common.config.Strings;
import com.starmao.scannable.common.item.ScannerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Renders a scan progress indicator overlay. */
public final class OverlayRenderer {
    private static final ResourceLocation PROGRESS =
            com.starmao.scannable.Scannable.id("textures/gui/overlay/scanner_progress.png");

    private static final ShaderProgram POSITION_TEX_SHADER = new ShaderProgram(
            ResourceLocation.withDefaultNamespace("position_tex"),
            DefaultVertexFormat.POSITION_TEX,
            ShaderDefines.EMPTY
    );

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) return;

        if (!ScannerItem.isScanner(stack)) return;

        int total = stack.getUseDuration(player);
        int remaining = player.getUseItemRemainingTicks();
        float progress = Mth.clamp(1 - (remaining - partialTick) / (float) total, 0, 1);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.setShader(POSITION_TEX_SHADER);
        RenderSystem.setShaderColor(0.66f, 0.8f, 0.93f, 0.66f);
        RenderSystem.setShaderTexture(0, PROGRESS);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        int size = 64;
        int midX = screenWidth / 2;
        int midY = screenHeight / 2;
        int left = midX - size / 2;
        int right = midX + size / 2;
        int top = midY - size / 2;
        int bottom = midY + size / 2;

        float angle = (float) (progress * Math.PI * 2);
        float tx = Mth.sin(angle);
        float ty = Mth.cos(angle);

        buffer.addVertex(midX, top, 0).setUv(0.5f, 1);
        if (progress < 0.125) { // Top right
            buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
            float x = tx / ty * 0.5f;
            buffer.addVertex(midX + x * size, top, 0).setUv(0.5f + x, 1);
        } else {
            buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
            buffer.addVertex(right, top, 0).setUv(1, 1);
            buffer.addVertex(right, top, 0).setUv(1, 1);
            if (progress < 0.375) { // Right
                buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                float y = Math.abs(ty / tx - 1) * 0.5f;
                buffer.addVertex(right, top + y * size, 0).setUv(1, 1 - y);
            } else {
                buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                buffer.addVertex(right, bottom, 0).setUv(1, 0);
                buffer.addVertex(right, bottom, 0).setUv(1, 0);
                if (progress < 0.625) { // Bottom
                    buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                    float x = Math.abs(tx / ty - 1) * 0.5f;
                    buffer.addVertex(left + x * size, bottom, 0).setUv(x, 0);
                } else {
                    buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                    buffer.addVertex(left, bottom, 0).setUv(0, 0);
                    buffer.addVertex(left, bottom, 0).setUv(0, 0);
                    if (progress < 0.875) { // Left
                        buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                        float y = (ty / tx + 1) * 0.5f;
                        buffer.addVertex(left, top + y * size, 0).setUv(0, 1 - y);
                    } else {
                        buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                        buffer.addVertex(left, top, 0).setUv(0, 1);
                        buffer.addVertex(left, top, 0).setUv(0, 1);
                        if (progress < 1) { // Top left
                            buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                            float x = Math.abs(tx / ty) * 0.5f;
                            buffer.addVertex(midX - x * size, top, 0).setUv(0.5f - x, 1);
                        } else {
                            buffer.addVertex(midX, midY, 0).setUv(0.5f, 0.5f);
                            buffer.addVertex(midX, top, 0).setUv(0.5f, 1);
                        }
                    }
                }
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        Component label = Strings.progress(Mth.floor(progress * 100));
        graphics.drawString(mc.font, label, right + 12, midY - mc.font.lineHeight / 2, 0xCCAACCEE, true);
    }

    private OverlayRenderer() {
    }
}
