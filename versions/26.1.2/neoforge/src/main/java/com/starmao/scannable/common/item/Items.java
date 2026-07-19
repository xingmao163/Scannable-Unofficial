package com.starmao.scannable.common.item;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.scanning.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;

public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Scannable.MOD_ID);

    public static final DeferredHolder<Item, ScannerItem> SCANNER = ITEMS.registerItem("scanner", ScannerItem::new);

    public static final DeferredHolder<Item, ScannerModuleItem> RANGE_MODULE = ITEMS.registerItem("range_module",
            properties -> new ScannerModuleItem(RangeScannerModule.INSTANCE, properties));
    public static final DeferredHolder<Item, ScannerModuleItem> FLUID_MODULE = ITEMS.registerItem("fluid_module",
            properties -> new ScannerModuleItem(FluidBlockScannerModule.INSTANCE, properties));
    public static final DeferredHolder<Item, ScannerModuleItem> FRIENDLY_ENTITY_MODULE = ITEMS.registerItem("friendly_entity_module",
            properties -> new ScannerModuleItem(FriendlyEntityScannerModule.INSTANCE, properties));
    public static final DeferredHolder<Item, ScannerModuleItem> HOSTILE_ENTITY_MODULE = ITEMS.registerItem("hostile_entity_module",
            properties -> new ScannerModuleItem(HostileEntityScannerModule.INSTANCE, properties));
    public static final DeferredHolder<Item, ConfigurableBlockScannerModuleItem> BLOCK_MODULE = ITEMS.registerItem("block_module",
            ConfigurableBlockScannerModuleItem::new);
    public static final DeferredHolder<Item, ConfigurableEntityScannerModuleItem> ENTITY_MODULE = ITEMS.registerItem("entity_module",
            ConfigurableEntityScannerModuleItem::new);
    public static final DeferredHolder<Item, ModItem> BLANK_MODULE = ITEMS.registerItem("blank_module",
            ModItem::new);
    public static final DeferredHolder<Item, ConfigurableItemScannerModuleItem> ITEM_MODULE = ITEMS.registerItem("item_module",
            ConfigurableItemScannerModuleItem::new);
    public static final DeferredHolder<Item, ScannerModuleItem> CHARGER_MODULE = ITEMS.registerItem("charger_module",
            properties -> new ScannerModuleItem(ChargingScannerModule.INSTANCE, properties));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private Items() {}
}
