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
    /** Side length of the source PNG canvas; the artwork occupies its top-left {@code imageWidth x imageHeight} pixels. */
    private static final int TEXTURE_SIZE = 256;
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
        // Sample only the top-left imageWidth x imageHeight pixels of the texture, which is where
        // the artwork actually lives; the source PNG is a 256x256 canvas. UVs are normalized, so
        // the region is divided by the texture size, and the destination is given in pixels via
        // the inclusive x0/y0/x1/y1 overload.
        //
        // Passing 0.0f..1.0f for both axes instead would stretch the whole canvas — mostly empty
        // margin — into this box, shrinking the drawn panel to about 176/256 of its intended size.
        final float u1 = (float) imageWidth / TEXTURE_SIZE;
        final float v1 = (float) imageHeight / TEXTURE_SIZE;
        graphics.blit(BACKGROUND, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight,
                0.0f, u1, 0.0f, v1);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.text(font, ACTIVE_TEXT, 8, 23, 0x404040, false);
        graphics.text(font, INACTIVE_TEXT, 8, 49, 0x404040, false);
    }

    /**
     * 阻止玩家把手持的 scanner 本体拖进它自己的槽位（否则会造成物品自吞）。
     *
     * <p>{@code slot.getItem() == scannerItemStack} 用引用比较即足够：同一格物品在
     * 同一 tick 内是同一个 {@link ItemStack} 实例。SWAP（数字键换位）要额外检查目标
     * 快捷栏格是否正好持有 scanner。
     */
    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ContainerInput type) {
        if (slot != null) {
            final ItemStack scannerItemStack = menu.getPlayer().getItemInHand(menu.getHand());
            if (slot.getItem() == scannerItemStack) return;
            if (type == ContainerInput.SWAP
                    && menu.getPlayer().getInventory().getItem(mouseButton) == scannerItemStack) {
                return;
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
