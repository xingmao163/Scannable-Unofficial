package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ScannerContainerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** The main scanner inventory screen. */
public class ScannerContainerScreen extends AbstractContainerScreen<ScannerContainerMenu> {
    private static final Identifier BACKGROUND =
            com.starmao.scannable.Scannable.id("textures/gui/container/scanner.png");
    private static final Component ACTIVE_TEXT = Component.translatable("gui.scannable_unofficial.scanner.active_modules");
    private static final Component ACTIVE_TOOLTIP = Component.translatable("gui.scannable_unofficial.scanner.active_modules.desc");
    private static final Component INACTIVE_TEXT = Component.translatable("gui.scannable_unofficial.scanner.inactive_modules");
    private static final Component INACTIVE_TOOLTIP = Component.translatable("gui.scannable_unofficial.scanner.inactive_modules.desc");

    public ScannerContainerScreen(final ScannerContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 176, 159);
        inventoryLabelX = 8;
        inventoryLabelY = 65;
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        final int x = (width - imageWidth) / 2;
        final int y = (height - imageHeight) / 2;
        graphics.blit(BACKGROUND, x, y, x + imageWidth, y + imageHeight, 0.0f, imageWidth / 256.0f, 0.0f, imageHeight / 256.0f);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        graphics.text(font, ACTIVE_TEXT, 8, 23, 0xFF404040, false);
        graphics.text(font, INACTIVE_TEXT, 8, 49, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (isHovering(8, 23, font.width(ACTIVE_TEXT), font.lineHeight, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(ACTIVE_TOOLTIP, mouseX, mouseY);
        }
        if (isHovering(8, 49, font.width(INACTIVE_TEXT), font.lineHeight, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(INACTIVE_TOOLTIP, mouseX, mouseY);
        }
    }

    @SuppressWarnings("null")
    @Override
    protected void slotClicked(@Nullable final Slot slot, final int slotId, final int mouseButton, final ContainerInput type) {
        if (slot != null) {
            final ItemStack scannerItemStack = menu.getPlayer().getItemInHand(menu.getHand());
            if (slot.getItem() == scannerItemStack) return;
            if (type == ContainerInput.SWAP && menu.getPlayer().getInventory().getItem(mouseButton) == scannerItemStack) return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
