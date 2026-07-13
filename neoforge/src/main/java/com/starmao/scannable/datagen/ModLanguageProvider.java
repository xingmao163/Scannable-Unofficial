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
    }
}
