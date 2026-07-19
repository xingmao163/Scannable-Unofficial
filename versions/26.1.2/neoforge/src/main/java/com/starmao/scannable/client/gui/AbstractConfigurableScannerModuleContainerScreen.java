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

import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.RemoveConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractConfigurableScannerModuleContainerScreen<TContainer extends AbstractModuleContainerMenu, TItem>
        extends AbstractContainerScreen<TContainer> {

    private static final Identifier BACKGROUND =
            com.starmao.scannable.Scannable.id("textures/gui/container/configurable_module.png");
    public static final int SLOTS_ORIGIN_X = 62;
    public static final int SLOTS_ORIGIN_Y = 20;
    public static final int SLOT_SIZE = 18;
    private static final int CONFIGURABLE_MODULE_SLOTS = 5;

    private final Component listCaption;
    private final Inventory inventory;

    public AbstractConfigurableScannerModuleContainerScreen(final TContainer container, final Inventory inventory,
                                                             final Component title, final Component listCaption) {
        super(container, inventory, title, 176, 133);
        this.listCaption = listCaption;
        this.inventory = inventory;
        inventoryLabelX = 8;
        inventoryLabelY = 39;
    }

    private ItemStack getHeldItem() {
        return menu.getPlayer().getItemInHand(menu.getHand());
    }

    protected abstract List<TItem> getConfiguredItems(ItemStack stack);
    protected abstract Component getItemName(TItem item);
    protected abstract void renderConfiguredItem(GuiGraphicsExtractor graphics, TItem item, int x, int y);

    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {}

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
        graphics.text(font, listCaption, 8, 23, 0xFF404040, false);

        final ItemStack stack = getHeldItem();
        final List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < CONFIGURABLE_MODULE_SLOTS; slot++) {
            final int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            final int y = SLOTS_ORIGIN_Y;

            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            }

            if (slot < items.size()) {
                renderConfiguredItem(graphics, items.get(slot), x, y);
            }
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        final ItemStack stack = getHeldItem();
        final List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < Math.min(items.size(), CONFIGURABLE_MODULE_SLOTS); slot++) {
            final int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            final int y = SLOTS_ORIGIN_Y;

            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(getItemName(items.get(slot)), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final double mouseX = event.x();
        final double mouseY = event.y();
        for (int slot = 0; slot < CONFIGURABLE_MODULE_SLOTS; slot++) {
            final int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            final int y = SLOTS_ORIGIN_Y;

            if (isHovering(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
                final ItemStack heldItemStack = menu.getCarried();
                if (!heldItemStack.isEmpty()) {
                    configureItemAt(getHeldItem(), slot, heldItemStack);
                } else {
                    Network.sendToServer(new RemoveConfiguredModuleItemAtMessage(menu.containerId, slot));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @SuppressWarnings("null")
    @Override
    protected void slotClicked(@Nullable final Slot slot, final int slotId, final int mouseButton, final ContainerInput type) {
        if (slot != null) {
            final ItemStack heldItem = getHeldItem();
            if (slot.getItem() == heldItem) return;
            if (type == ContainerInput.SWAP && inventory.getItem(mouseButton) == heldItem) return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
