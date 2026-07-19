package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ItemModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Screen for configuring an item scanner module. */
public class ConfigurableItemScannerModuleContainerScreen
        extends AbstractConfigurableScannerModuleContainerScreen<ItemModuleContainerMenu, Item> {

    public ConfigurableItemScannerModuleContainerScreen(final ItemModuleContainerMenu container,
                                                         final Inventory inventory, final Component title) {
        super(container, inventory, title,
                Component.translatable("gui.scannable_unofficial.scanner.item_module.list"));
    }

    @Override
    protected List<Item> getConfiguredItems(final ItemStack stack) {
        if (stack.getItem() instanceof ConfigurableItemScannerModuleItem item) {
            return item.getValues(stack);
        }
        return List.of();
    }

    @Override
    protected Component getItemName(final Item item) {
        return item.getName(item.getDefaultInstance());
    }

    @Override
    protected void renderConfiguredItem(final GuiGraphicsExtractor graphics, final Item item, final int x, final int y) {
        graphics.item(new ItemStack(item), x, y);
    }

    @Override
    protected void configureItemAt(final ItemStack stack, final int slot, final ItemStack value) {
        if (!value.isEmpty()) {
            BuiltInRegistries.ITEM.getResourceKey(value.getItem()).ifPresent(key ->
                    Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.identifier())));
        }
    }
}
