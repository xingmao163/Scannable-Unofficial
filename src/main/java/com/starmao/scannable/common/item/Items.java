package com.starmao.scannable.common.item;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.scanning.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Scannable.MOD_ID);

    public static final DeferredItem<ScannerItem> SCANNER = ITEMS.register("scanner", ScannerItem::new);

    public static final DeferredItem<ScannerModuleItem> RANGE_MODULE = ITEMS.register("range_module",
            () -> new ScannerModuleItem(RangeScannerModule.INSTANCE));
    public static final DeferredItem<ScannerModuleItem> FLUID_MODULE = ITEMS.register("fluid_module",
            () -> new ScannerModuleItem(FluidBlockScannerModule.INSTANCE));
    public static final DeferredItem<ScannerModuleItem> FRIENDLY_ENTITY_MODULE = ITEMS.register("friendly_entity_module",
            () -> new ScannerModuleItem(FriendlyEntityScannerModule.INSTANCE));
    public static final DeferredItem<ScannerModuleItem> HOSTILE_ENTITY_MODULE = ITEMS.register("hostile_entity_module",
            () -> new ScannerModuleItem(HostileEntityScannerModule.INSTANCE));
    public static final DeferredItem<ConfigurableBlockScannerModuleItem> BLOCK_MODULE = ITEMS.register("block_module",
            () -> new ConfigurableBlockScannerModuleItem());
    public static final DeferredItem<ConfigurableEntityScannerModuleItem> ENTITY_MODULE = ITEMS.register("entity_module",
            () -> new ConfigurableEntityScannerModuleItem());
    public static final DeferredItem<ModItem> BLANK_MODULE = ITEMS.register("blank_module", () -> new ModItem());
    public static final DeferredItem<ConfigurableItemScannerModuleItem> ITEM_MODULE = ITEMS.register("item_module",
            () -> new ConfigurableItemScannerModuleItem());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private Items() {}
}
