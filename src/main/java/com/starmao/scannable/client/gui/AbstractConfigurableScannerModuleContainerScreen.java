package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.RemoveConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
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
    /** Side length of the source PNG canvas; the artwork occupies its top-left {@code imageWidth x imageHeight} pixels. */
    private static final int TEXTURE_SIZE = 256;
    public static final int SLOTS_ORIGIN_X = 62;
    public static final int SLOTS_ORIGIN_Y = 20;
    /** Horizontal pitch between adjacent slots; also the slot frame pitch in the texture. */
    public static final int SLOT_SIZE = 18;
    /** Number of filter slots drawn per row. Shared with the JEI ghost-drag targets. */
    public static final int SLOT_COUNT = 5;
    /** Hover target for a slot. */
    private static final int SLOT_HIT_SIZE = 16;
    private static final Identifier SLOT_HIGHLIGHT_SPRITE =
            Identifier.withDefaultNamespace("container/slot_highlight_back");
    /** Source sprite edge length. */
    private static final int SLOT_HIGHLIGHT_SPRITE_SIZE = 24;
    /**
     * Draw offset from the slot's origin to the sprite's top-left corner.
     *
     * <p>The sprite is 24x24 and centred on an 18x18 slot, so it overhangs by
     * {@code (24 - 18) / 2 = 3} on each side; vanilla additionally shifts by one, giving -4.
     */
    private static final int SLOT_HIGHLIGHT_OFFSET = -4;

    private final Component listCaption;
    private final Inventory inventory;

    public AbstractConfigurableScannerModuleContainerScreen(TContainer container, Inventory inventory,
                                                             Component title, Component listCaption) {
        super(container, inventory, title, 176, 133);
        this.listCaption = listCaption;
        this.inventory = inventory;
    }

    /** The scanner item the player is currently configuring. */
    protected ItemStack getHeldItem() {
        return menu.getPlayer().getItemInHand(menu.getHand());
    }

    protected abstract List<TItem> getConfiguredItems(ItemStack stack);
    protected abstract Component getItemName(TItem item);
    protected abstract void renderConfiguredItem(GuiGraphicsExtractor graphics, TItem item, int x, int y);

    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {}

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

    private void renderSlotHighlights(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack stack = getHeldItem();
        List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < Math.min(items.size(), SLOT_COUNT); slot++) {
            int x = SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            int y = SLOTS_ORIGIN_Y;
            if (isHovering(x, y, SLOT_HIT_SIZE, SLOT_HIT_SIZE, mouseX, mouseY)) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_SPRITE,
                        x + SLOT_HIGHLIGHT_OFFSET, y + SLOT_HIGHLIGHT_OFFSET,
                        SLOT_HIGHLIGHT_SPRITE_SIZE, SLOT_HIGHLIGHT_SPRITE_SIZE);
            }
        }
    }

    /**
     * 模块物品槽位的交互入口：手持物品时写入该槽位的配置，空手时清除该槽位。
     *
     * <p>这些槽位不是 vanilla 的 {@link net.minecraft.world.inventory.Slot}，而是画在
     * GUI 上的自绘区域，所以要在这里按坐标自行命中检测，不能依赖 {@code slotClicked}。
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        final double mouseX = event.x();
        final double mouseY = event.y();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
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

    /**
     * 阻止玩家把手持的 scanner 本体拖进它自己的模块槽位（否则会造成物品自吞）。
     */
    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ContainerInput type) {
        if (slot != null) {
            final ItemStack heldItem = getHeldItem();
            if (slot.getItem() == heldItem) return;
            if (type == ContainerInput.SWAP && inventory.getItem(mouseButton) == heldItem) return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.text(font, listCaption, 8, 23, 0x404040, false);

        ItemStack stack = getHeldItem();
        List<TItem> items = getConfiguredItems(stack);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (slot < items.size()) {
                renderConfiguredItem(graphics, items.get(slot),
                        SLOTS_ORIGIN_X + slot * SLOT_SIZE, SLOTS_ORIGIN_Y);
            }
        }

        renderSlotHighlights(graphics, mouseX, mouseY);
    }
}
