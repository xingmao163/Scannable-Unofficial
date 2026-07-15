package com.starmao.scannable.integration.emi;

import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * EMI integration plugin for Scannable-Unofficial.
 * Registers drag-drop handlers for the configurable module screens.
 */
@EmiEntrypoint
public class ScannableEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(
            ConfigurableBlockScannerModuleContainerScreen.class,
            new BlockModuleEmiHandler());
        registry.addDragDropHandler(
            ConfigurableEntityScannerModuleContainerScreen.class,
            new EntityModuleEmiHandler());
        registry.addDragDropHandler(
            ConfigurableItemScannerModuleContainerScreen.class,
            new ItemModuleEmiHandler());
    }
}
