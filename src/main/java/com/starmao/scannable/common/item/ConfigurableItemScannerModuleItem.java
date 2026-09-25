package com.starmao.scannable.common.item;

import com.starmao.scannable.common.container.ItemModuleContainerMenu;
import com.starmao.scannable.common.scanning.ItemScannerModule;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Configurable item scanner module item — 1.21.2 variant with Properties constructor.
 */
public final class ConfigurableItemScannerModuleItem extends ConfigurableModuleItem<Item> {
    public ConfigurableItemScannerModuleItem() {
        super(ItemScannerModule.INSTANCE,
                (id, inv, hand) -> new ItemModuleContainerMenu(id, inv, hand));
    }

    public ConfigurableItemScannerModuleItem(Item.Properties properties) {
        super(ItemScannerModule.INSTANCE,
                (id, inv, hand) -> new ItemModuleContainerMenu(id, inv, hand),
                properties);
    }

    @Override
    protected DataComponentType<List<ResourceLocation>> getComponent() {
        return ModDataComponents.SCAN_ITEMS.get();
    }

    @Override
    protected Registry<Item> getRegistry() {
        return BuiltInRegistries.ITEM;
    }

    @Override
    protected Component getDisplayName(final Item item) {
        return item.getName(item.getDefaultInstance());
    }

    @Override
    protected Component getListCaption() {
        return Component.translatable("tooltip.scannable_unofficial.scanner.item_module.list")
                .withStyle(net.minecraft.ChatFormatting.GRAY);
    }
}
