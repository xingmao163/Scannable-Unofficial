package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ItemModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ConfigurableItemScannerModuleContainerScreen
        extends AbstractConfigurableScannerModuleContainerScreen<ItemModuleContainerMenu, Item> {

    public ConfigurableItemScannerModuleContainerScreen(ItemModuleContainerMenu container,
                                                        Inventory inventory, Component title) {
        super(container, inventory, title,
                Component.translatable("gui.scannable_unofficial.scanner.item_module.list"));
    }

    @Override
    protected List<Item> getConfiguredItems(ItemStack stack) {
        if (stack.getItem() instanceof ConfigurableItemScannerModuleItem item) {
            return item.getValues(stack);
        }
        return List.of();
    }

    @Override
    protected Component getItemName(Item item) {
        return item.getName(item.getDefaultInstance());
    }

    @Override
    protected void renderConfiguredItem(GuiGraphics graphics, Item item, int x, int y) {
        graphics.renderFakeItem(new ItemStack(item), x, y);
    }

    @Override
    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {
        if (!value.isEmpty()) {
            BuiltInRegistries.ITEM.getResourceKey(value.getItem()).ifPresent(key ->
                    Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.location())));
        }
    }
}
