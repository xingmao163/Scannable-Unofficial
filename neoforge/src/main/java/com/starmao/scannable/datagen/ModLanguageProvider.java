package com.starmao.scannable.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generates {@code assets/scannable_unofficial/lang/en_us.json}.
 */
public final class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(final PackOutput output, final String modId, final String locale) {
        super(output, modId, locale);
    }

    @Override
    protected void addTranslations() {
        // Creative tab
        add("itemGroup.scannable_unofficial", "Scannable");

        // Items
        add("item.scannable_unofficial.scanner", "Scanner");
        add("item.scannable_unofficial.range_module", "Range Module");
        add("item.scannable_unofficial.fluid_module", "Fluid Module");
        add("item.scannable_unofficial.friendly_entity_module", "Friendly Entity Module");
        add("item.scannable_unofficial.hostile_entity_module", "Hostile Entity Module");
        add("item.scannable_unofficial.entity_module", "Entity Module");
        add("item.scannable_unofficial.block_module", "Block Module");
        add("item.scannable_unofficial.item_module", "Item Module");
        add("item.scannable_unofficial.blank_module", "Blank Module");

        // Item descriptions
        add("item.scannable_unofficial.range_module.desc", "Increases the scan radius by 50%%");
        add("item.scannable_unofficial.fluid_module.desc", "Detects fluid blocks such as water and lava");
        add("item.scannable_unofficial.friendly_entity_module.desc", "Detects friendly entities such as animals and villagers");
        add("item.scannable_unofficial.hostile_entity_module.desc", "Detects hostile entities such as monsters");
        add("item.scannable_unofficial.entity_module.desc", "Right-click entity to configure specific entity types to detect");
        add("item.scannable_unofficial.block_module.desc", "Right-click block to configure specific blocks to detect");
        add("item.scannable_unofficial.blank_module.desc", "A placeholder module with no function");
        add("item.scannable_unofficial.item_module.desc", "Scans containers for configured items");

        // Container titles
        add("container.scannable_unofficial.scanner", "Scanner");
        add("container.scannable_unofficial.block_module", "Block Module");
        add("container.scannable_unofficial.entity_module", "Entity Module");

        // GUI labels
        add("gui.scannable_unofficial.scanner.active_modules", "Modules");
        add("gui.scannable_unofficial.scanner.inactive_modules", "Inactive");
        add("gui.scannable_unofficial.scanner.progress", "%s%%");
        add("gui.scannable_unofficial.scanner.overlay.distance", "%s (%sm)");
        add("gui.scannable_unofficial.scanner.block_module.list", "Blocks");
        add("gui.scannable_unofficial.scanner.entity_module.list", "Entities");
        add("gui.scannable_unofficial.scanner.item_module.list", "Items");

        // Tooltips
        add("tooltip.scannable_unofficial.scanner.energy", "Energy: %s / %s FE");
        add("tooltip.scannable_unofficial.scanner_module.energy", "Energy cost: %s FE");
        add("tooltip.scannable_unofficial.scanner.total_energy_cost", "Total: %s FE per scan");
        add("tooltip.scannable_unofficial.scanner.active_modules", "Active Modules");
        add("tooltip.scannable_unofficial.scanner.inactive_modules", "Inactive");
        add("tooltip.scannable_unofficial.scanner.block_module.list", "Selected blocks:");
        add("tooltip.scannable_unofficial.scanner.entity_module.list", "Selected entities:");
        add("tooltip.scannable_unofficial.scanner.item_module.list", "Selected items:");

        // Messages
        add("message.scannable_unofficial.scanner.no_target_items", "§cItem module has no items configured! Right-click to open config");
        add("message.scannable_unofficial.scanner.no_modules", "No scan modules installed!");
        add("message.scannable_unofficial.scanner.no_energy", "Not enough energy!");
        add("message.scannable_unofficial.scanner.no_free_slots", "No free slots!");

        // Configuration screen — ModConfig (scannable_unofficial-server.toml)
        add("scannable_unofficial.configuration.debug", "Debug");
        add("scannable_unofficial.configuration.debug.logItemScanner", "Log Item Scanner");
        add("scannable_unofficial.configuration.scanner", "Scanner");
        add("scannable_unofficial.configuration.scanner.useEnergy", "Use Energy");
        add("scannable_unofficial.configuration.scanner.energyCapacity", "Energy Capacity");
        add("scannable_unofficial.configuration.scanner.baseScanRadius", "Base Scan Radius");
        add("scannable_unofficial.configuration.scanner.resultStayDuration", "Result Stay Duration");
        add("scannable_unofficial.configuration.energy", "Energy Cost");
        add("scannable_unofficial.configuration.energy.range", "Range Module");
        add("scannable_unofficial.configuration.energy.fluid", "Fluid Module");
        add("scannable_unofficial.configuration.energy.friendly", "Friendly Entity Module");
        add("scannable_unofficial.configuration.energy.hostile", "Hostile Entity Module");
        add("scannable_unofficial.configuration.energy.block", "Block Module");
        add("scannable_unofficial.configuration.energy.entity", "Entity Module");
        add("scannable_unofficial.configuration.energy.item", "Item Module");
        add("scannable_unofficial.configuration.range", "Range Modifiers");
        add("scannable_unofficial.configuration.range.range", "Range Module Modifier");
        add("scannable_unofficial.configuration.range.fluid", "Fluid Module Modifier");
        add("scannable_unofficial.configuration.range.block", "Block Module Modifier");
        add("scannable_unofficial.configuration.fluids", "Fluids");
        add("scannable_unofficial.configuration.fluids.ignoredTags", "Ignored Fluid Tags");
        add("scannable_unofficial.configuration.ignored", "Ignored Blocks");
        add("scannable_unofficial.configuration.ignored.blocks", "Ignored Block Registry Names");
        add("scannable_unofficial.configuration.ignored.blockTags", "Ignored Block Tags");

        // Configuration screen — ClientConfig (scannable_unofficial-client.toml)
        add("scannable_unofficial.configuration.colors", "Colors");
        add("scannable_unofficial.configuration.colors.blocksColors", "Block Colors");
        add("scannable_unofficial.configuration.colors.blockTagsColors", "Block Tag Colors");
        add("scannable_unofficial.configuration.colors.fluidsColors", "Fluid Colors");
        add("scannable_unofficial.configuration.colors.fluidTagsColors", "Fluid Tag Colors");
        add("scannable_unofficial.configuration.misc", "Misc");
        add("scannable_unofficial.configuration.misc.hideBrokenBlocks", "Hide Broken Blocks");
        add("scannable_unofficial.configuration.misc.itemScanColor", "Item Scan Color");
    }
}
