package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ScannerContainerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The main scanner inventory screen. */
public class ScannerContainerScreen extends AbstractContainerScreen<ScannerContainerMenu> {
    private static final Identifier BACKGROUND =
            com.starmao.scannable.Scannable.id("textures/gui/container/scanner.png");
    private static final Component ACTIVE_TEXT = Component.translatable("gui.scannable_unofficial.scanner.active_modules");
    private static final Component INACTIVE_TEXT = Component.translatable("gui.scannable_unofficial.scanner.inactive_modules");

    public ScannerContainerScreen(ScannerContainerMenu container, Inventory inventory, Component title) {
        super(container, inventory, title, 176, 159);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(BACKGROUND, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.text(font, ACTIVE_TEXT, 8, 23, 0x404040, false);
        graphics.text(font, INACTIVE_TEXT, 8, 49, 0x404040, false);
    }
}
