package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.EntityModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.Holder;

import java.util.List;

/** Screen for configuring an entity scanner module. */
public class ConfigurableEntityScannerModuleContainerScreen
        extends AbstractConfigurableScannerModuleContainerScreen<EntityModuleContainerMenu, EntityType<?>> {

    public ConfigurableEntityScannerModuleContainerScreen(EntityModuleContainerMenu container,
                                                           Inventory inventory, Component title) {
        super(container, inventory, title,
                Component.translatable("gui.scannable_unofficial.scanner.entity_module.list"));
    }

    protected ConfigurableEntityScannerModuleContainerScreen(EntityModuleContainerMenu container,
                                                              Inventory inventory, Component title, Component listCaption) {
        super(container, inventory, title, listCaption);
    }

    @Override
    protected List<EntityType<?>> getConfiguredItems(ItemStack stack) {
        if (stack.getItem() instanceof ConfigurableEntityScannerModuleItem item) {
            return item.getValues(stack);
        }
        return List.of();
    }

    @Override
    protected Component getItemName(EntityType<?> entityType) {
        return entityType.getDescription();
    }

    @Override
    protected void renderConfiguredItem(GuiGraphicsExtractor graphics, EntityType<?> entityType, int x, int y) {
        ItemStack eggStack = SpawnEggItem.byId(entityType).map(Holder::value).map(ItemStack::new).orElse(ItemStack.EMPTY);
        graphics.fakeItem(eggStack, x, y);
    }

    @Override
    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {
        if (value.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> entityType = egg.getType(value);
            BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).ifPresent(key ->
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.identifier())));
        }
    }
}
