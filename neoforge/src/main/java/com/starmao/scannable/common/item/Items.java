package com.starmao.scannable.common.item;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.scanning.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Registration hub for all scanner items.
 * <p>Defines every item in the mod via NeoForge's {@link DeferredRegister} system.
 * Items are registered with the mod event bus via {@link #register(IEventBus)}.
 */
public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Scannable.MOD_ID);

    /** The handheld scanner item — core item of the mod. */
    public static final DeferredItem<ScannerItem> SCANNER = ITEMS.registerItem("scanner",
            ScannerItem::new, p -> p.stacksTo(1));

    // ---- Scanner Modules ---- //

    /** Increases scan radius by a configurable percentage. */
    public static final DeferredItem<ScannerModuleItem> RANGE_MODULE = ITEMS.registerItem("range_module",
            props -> new ScannerModuleItem(props, RangeScannerModule.INSTANCE), p -> p.stacksTo(1));
    /** Detects fluid blocks (water, lava, etc.). */
    public static final DeferredItem<ScannerModuleItem> FLUID_MODULE = ITEMS.registerItem("fluid_module",
            props -> new ScannerModuleItem(props, FluidBlockScannerModule.INSTANCE), p -> p.stacksTo(1));
    /** Detects friendly entities (animals, villagers). */
    public static final DeferredItem<ScannerModuleItem> FRIENDLY_ENTITY_MODULE = ITEMS.registerItem("friendly_entity_module",
            props -> new ScannerModuleItem(props, FriendlyEntityScannerModule.INSTANCE), p -> p.stacksTo(1));
    /** Detects hostile entities (monsters). */
    public static final DeferredItem<ScannerModuleItem> HOSTILE_ENTITY_MODULE = ITEMS.registerItem("hostile_entity_module",
            props -> new ScannerModuleItem(props, HostileEntityScannerModule.INSTANCE), p -> p.stacksTo(1));
    /** Configurable module — detects specific blocks configured by the player. */
    public static final DeferredItem<ConfigurableBlockScannerModuleItem> BLOCK_MODULE = ITEMS.registerItem("block_module",
            ConfigurableBlockScannerModuleItem::new, p -> p.stacksTo(1));
    /** Configurable module — detects specific entity types configured by the player. */
    public static final DeferredItem<ConfigurableEntityScannerModuleItem> ENTITY_MODULE = ITEMS.registerItem("entity_module",
            ConfigurableEntityScannerModuleItem::new, p -> p.stacksTo(1));
    /** Placeholder module item with no scanning function (for recipe purposes). */
    public static final DeferredItem<ModItem> BLANK_MODULE = ITEMS.registerItem("blank_module",
            ModItem::new, p -> p);
    /** Configurable module — detects specific items in containers. */
    public static final DeferredItem<ConfigurableItemScannerModuleItem> ITEM_MODULE = ITEMS.registerItem("item_module",
            ConfigurableItemScannerModuleItem::new, p -> p.stacksTo(1));
    /** Detects common ore blocks (coal, iron, copper, gold, lapis, redstone). */
    public static final DeferredItem<ScannerModuleItem> COMMON_ORES_MODULE = ITEMS.registerItem("common_ores_module",
            props -> new ScannerModuleItem(props, CommonOresBlockScannerModule.INSTANCE), p -> p.stacksTo(1));
    /** Detects rare ore blocks (diamond, emerald, netherite, quartz, etc.). */
    public static final DeferredItem<ScannerModuleItem> RARE_ORES_MODULE = ITEMS.registerItem("rare_ores_module",
            props -> new ScannerModuleItem(props, RareOresBlockScannerModule.INSTANCE), p -> p.stacksTo(1));
    /** Recharges scanners placed in the charging slot. */
    public static final DeferredItem<ScannerModuleItem> CHARGER_MODULE = ITEMS.registerItem("charger_module",
            props -> new ScannerModuleItem(props, ChargingScannerModule.INSTANCE), p -> p.stacksTo(1));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private Items() {}
}
