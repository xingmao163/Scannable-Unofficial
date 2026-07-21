/**
 * Base screen for configurable scanner module GUIs (block, entity, item).
 * <p>Renders a list of configured target entries with slots, supports
 * clicking a slot to configure via JEI ghost-drag or direct slot interaction.
 * Subclasses supply the specific item type, rendering, and network handling.
 *
 * @param <TContainer> the container menu type
 * @param <TItem>      the configured item type (e.g. {@link net.minecraft.world.level.block.Block})
 */
package com.starmao.scannable.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.RemoveConfiguredModuleItemAtMessage;
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
import java.util.List;

public abstract class AbstractConfigurableScannerModuleContainerScreen<TContainer extends AbstractModuleContainerMenu, TItem>
        extends AbstractContainerScreen<TContainer> {

    private static final ResourceLocation BACKGROUND =
            com.starmao.scannable.Scannable.id("textures/gui/container/configurable_module.png");
    public static final int SLOTS_ORIGIN_X = 62;
    public static final int SLOTS_ORIGIN_Y = 20;
    public static final int SLOT_SIZE = 18;

    private final Component listCaption;
    private final Inventory inventory;

    public AbstractConfigurableScannerModuleContainerScreen(TContainer container, Inventory inventory,
                                                             Component title, Component listCaption) {
        super(container, inventory, title);
        this.listCaption = listCaption;
        this.inventory = inventory;
        this.imageHeight = 133;
    }

    private ItemStack getHeldItem() {
        return menu.getPlayer().getItemInHand(menu.getHand());
    }

    protected abstract List<TItem> getConfiguredItems(ItemStack stack);
    protected abstract Component getItemName(TItem item);
    protected abstract void renderConfiguredItem(GuiGraphics graphics, TItem item, int x, int y);

    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);

        ItemStack stack = getHeldItem();
        List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < Math.min(items.size(), 5); slot++) {
            int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            int y = SLOTS_ORIGIN_Y;
            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                TItem item = items.get(slot);
                graphics.renderTooltip(font, getItemName(item), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, listCaption, 8, 23, 0x404040, false);

        ItemStack stack = getHeldItem();
        List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < 5; slot++) {
            int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            int y = SLOTS_ORIGIN_Y;
            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                // renderSlotHighlight(graphics, x, y, SLOT_SIZE, 400);
            }
            if (slot < items.size()) {
                renderConfiguredItem(graphics, items.get(slot), x, y);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderType::guiTextured, BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int slot = 0; slot < 5; slot++) {
            int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            int y = SLOTS_ORIGIN_Y;
            if (isHovering(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
                ItemStack heldItemStack = menu.getCarried();
                if (!heldItemStack.isEmpty()) {
                    configureItemAt(getHeldItem(), slot, heldItemStack);
                } else {
                    Network.sendToServer(new RemoveConfiguredModuleItemAtMessage(menu.containerId, slot));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null) {
            ItemStack heldItem = getHeldItem();
            if (slot.getItem() == heldItem) return;
            if (type == ClickType.SWAP && inventory.getItem(mouseButton) == heldItem) return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
