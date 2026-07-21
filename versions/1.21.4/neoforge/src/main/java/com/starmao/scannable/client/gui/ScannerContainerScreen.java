package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ScannerContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** The main scanner inventory screen. */
public class ScannerContainerScreen extends AbstractContainerScreen<ScannerContainerMenu> {
    private static final ResourceLocation BACKGROUND =
            com.starmao.scannable.Scannable.id("textures/gui/container/scanner.png");
    private static final Component ACTIVE_TEXT = Component.translatable("gui.scannable_unofficial.scanner.active_modules");
    private static final Component INACTIVE_TEXT = Component.translatable("gui.scannable_unofficial.scanner.inactive_modules");

    public ScannerContainerScreen(ScannerContainerMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageHeight = 159;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, ACTIVE_TEXT, 8, 23, 0x404040, false);
        graphics.drawString(font, INACTIVE_TEXT, 8, 49, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderType::guiTextured, BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null) {
            ItemStack scannerItemStack = menu.getPlayer().getItemInHand(menu.getHand());
            if (slot.getItem() == scannerItemStack) return;
            if (type == ClickType.SWAP && menu.getPlayer().getInventory().getItem(mouseButton) == scannerItemStack) return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
