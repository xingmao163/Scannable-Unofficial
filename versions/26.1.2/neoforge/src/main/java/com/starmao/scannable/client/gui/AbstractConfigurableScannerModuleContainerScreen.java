package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public abstract class AbstractConfigurableScannerModuleContainerScreen<TContainer extends AbstractModuleContainerMenu, TItem>
        extends AbstractContainerScreen<TContainer> {

    private static final Identifier BACKGROUND =
            com.starmao.scannable.Scannable.id("textures/gui/container/configurable_module.png");
    public static final int SLOTS_ORIGIN_X = 62;
    public static final int SLOTS_ORIGIN_Y = 20;
    public static final int SLOT_SIZE = 18;

    private final Component listCaption;
    private final Inventory inventory;

    public AbstractConfigurableScannerModuleContainerScreen(TContainer container, Inventory inventory,
                                                             Component title, Component listCaption) {
        super(container, inventory, title, 176, 133);
        this.listCaption = listCaption;
        this.inventory = inventory;
    }

    private ItemStack getHeldItem() {
        return menu.getPlayer().getItemInHand(menu.getHand());
    }

    protected abstract List<TItem> getConfiguredItems(ItemStack stack);
    protected abstract Component getItemName(TItem item);
    protected abstract void renderConfiguredItem(GuiGraphicsExtractor graphics, TItem item, int x, int y);

    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        renderSlotHighlights(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(BACKGROUND, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    private void renderSlotHighlights(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack stack = getHeldItem();
        List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < Math.min(items.size(), 5); slot++) {
            int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            int y = SLOTS_ORIGIN_Y;
            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                graphics.outline(x, y, 16, 16, 0x80FFFFFF);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.text(font, listCaption, 8, 23, 0x404040, false);

        ItemStack stack = getHeldItem();
        List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < 5; slot++) {
            int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            int y = SLOTS_ORIGIN_Y;
            if (slot < items.size()) {
                renderConfiguredItem(graphics, items.get(slot), x, y);
            }
        }
    }
}
