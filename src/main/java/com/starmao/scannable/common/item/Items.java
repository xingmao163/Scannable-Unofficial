package com.starmao.scannable.common.item;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.scanning.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration hub for all scanner items.
 * <p>Defines every item in the mod via NeoForge's {@link DeferredRegister} system.
 * Items are registered with the mod event bus via {@link #register(IEventBus)}.
 */
public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Scannable.MOD_ID);

    /** The handheld scanner item — core item of the mod. */
    public static final DeferredItem<ScannerItem> SCANNER = ITEMS.register("scanner", ScannerItem::new);

    // ---- Scanner Modules ---- //

    /** Increases scan radius by a configurable percentage. */
    public static final DeferredItem<ScannerModuleItem> RANGE_MODULE = ITEMS.register("range_module",
            () -> new ScannerModuleItem(RangeScannerModule.INSTANCE));
    /** Detects fluid blocks (water, lava, etc.). */
    public static final DeferredItem<ScannerModuleItem> FLUID_MODULE = ITEMS.register("fluid_module",
            () -> new ScannerModuleItem(FluidBlockScannerModule.INSTANCE));
    /** Detects friendly entities (animals, villagers). */
    public static final DeferredItem<ScannerModuleItem> FRIENDLY_ENTITY_MODULE = ITEMS.register("friendly_entity_module",
            () -> new ScannerModuleItem(FriendlyEntityScannerModule.INSTANCE));
    /** Detects hostile entities (monsters). */
    public static final DeferredItem<ScannerModuleItem> HOSTILE_ENTITY_MODULE = ITEMS.register("hostile_entity_module",
            () -> new ScannerModuleItem(HostileEntityScannerModule.INSTANCE));
    /** Configurable module — detects specific blocks configured by the player. */
    public static final DeferredItem<ConfigurableBlockScannerModuleItem> BLOCK_MODULE = ITEMS.register("block_module",
            () -> new ConfigurableBlockScannerModuleItem());
    /** Configurable module — detects specific entity types configured by the player. */
    public static final DeferredItem<ConfigurableEntityScannerModuleItem> ENTITY_MODULE = ITEMS.register("entity_module",
            () -> new ConfigurableEntityScannerModuleItem());
    /** Placeholder module item with no scanning function (for recipe purposes). */
    public static final DeferredItem<ModItem> BLANK_MODULE = ITEMS.register("blank_module", () -> new ModItem());
    /** Configurable module — detects specific items in containers. */
    public static final DeferredItem<ConfigurableItemScannerModuleItem> ITEM_MODULE = ITEMS.register("item_module",
            () -> new ConfigurableItemScannerModuleItem());
    /** Detects common ore blocks (coal, iron, copper, gold, lapis, redstone). */
    public static final DeferredItem<ScannerModuleItem> COMMON_ORES_MODULE = ITEMS.register("common_ores_module",
            () -> new ScannerModuleItem(CommonOresBlockScannerModule.INSTANCE));
    /** Detects rare ore blocks (diamond, emerald, netherite, quartz, etc.). */
    public static final DeferredItem<ScannerModuleItem> RARE_ORES_MODULE = ITEMS.register("rare_ores_module",
            () -> new ScannerModuleItem(RareOresBlockScannerModule.INSTANCE));
    /** Generates energy for the scanner over time when installed. */
    public static final DeferredItem<ScannerModuleItem> CHARGER_MODULE = ITEMS.register("charger_module",
            () -> new ScannerModuleItem(ChargingScannerModule.INSTANCE));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private Items() {}
}
