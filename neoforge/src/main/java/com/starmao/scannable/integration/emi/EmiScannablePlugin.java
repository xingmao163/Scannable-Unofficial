package com.starmao.scannable.integration.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiEntrypoint;

/**
 * EMI integration plugin for Scannable Unofficial.
 *
 * <p>Registers drag-drop handlers so players can drag items/blocks/entities
 * from EMI into module configuration slots.
 */
@EmiEntrypoint
public class EmiScannablePlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericDragDropHandler(new ModuleSlotDragHandler());
    }
}
