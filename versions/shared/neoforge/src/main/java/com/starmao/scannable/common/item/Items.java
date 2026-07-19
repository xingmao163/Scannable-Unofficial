package com.starmao.scannable.common.item;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.scanning.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Scannable.MOD_ID);

    public static final DeferredItem<ScannerItem> SCANNER = ITEMS.register("scanner",
            (Function<ResourceLocation, ScannerItem>) loc ->
                    new ScannerItem(ModItem.props(loc, p -> p.stacksTo(1))));

    public static final DeferredItem<ScannerModuleItem> RANGE_MODULE = ITEMS.register("range_module",
            (Function<ResourceLocation, ScannerModuleItem>) loc ->
                    new ScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1)), RangeScannerModule.INSTANCE));
    public static final DeferredItem<ScannerModuleItem> FLUID_MODULE = ITEMS.register("fluid_module",
            (Function<ResourceLocation, ScannerModuleItem>) loc ->
                    new ScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1)), FluidBlockScannerModule.INSTANCE));
    public static final DeferredItem<ScannerModuleItem> FRIENDLY_ENTITY_MODULE = ITEMS.register("friendly_entity_module",
            (Function<ResourceLocation, ScannerModuleItem>) loc ->
                    new ScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1)), FriendlyEntityScannerModule.INSTANCE));
    public static final DeferredItem<ScannerModuleItem> HOSTILE_ENTITY_MODULE = ITEMS.register("hostile_entity_module",
            (Function<ResourceLocation, ScannerModuleItem>) loc ->
                    new ScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1)), HostileEntityScannerModule.INSTANCE));
    public static final DeferredItem<ConfigurableBlockScannerModuleItem> BLOCK_MODULE = ITEMS.register("block_module",
            (Function<ResourceLocation, ConfigurableBlockScannerModuleItem>) loc ->
                    new ConfigurableBlockScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1))));
    public static final DeferredItem<ConfigurableEntityScannerModuleItem> ENTITY_MODULE = ITEMS.register("entity_module",
            (Function<ResourceLocation, ConfigurableEntityScannerModuleItem>) loc ->
                    new ConfigurableEntityScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1))));
    public static final DeferredItem<ModItem> BLANK_MODULE = ITEMS.register("blank_module",
            (Function<ResourceLocation, ModItem>) loc ->
                    new ModItem(ModItem.props(loc, null)));
    public static final DeferredItem<ConfigurableItemScannerModuleItem> ITEM_MODULE = ITEMS.register("item_module",
            (Function<ResourceLocation, ConfigurableItemScannerModuleItem>) loc ->
                    new ConfigurableItemScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1))));
    public static final DeferredItem<ScannerModuleItem> CHARGER_MODULE = ITEMS.register("charger_module",
            (Function<ResourceLocation, ScannerModuleItem>) loc ->
                    new ScannerModuleItem(ModItem.props(loc, p -> p.stacksTo(1)), ChargingScannerModule.INSTANCE));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private Items() {}
}
