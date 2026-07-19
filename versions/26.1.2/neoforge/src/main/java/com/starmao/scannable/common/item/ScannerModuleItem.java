package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Item wrapper for a non-configurable scanner module.
 * <p>Each instance holds a reference to its {@link ScannerModule} implementation
 * (e.g. {@link com.starmao.scannable.common.scanning.RangeScannerModule}).
 * The module behaviour is accessed via {@link #getModule()}.
 */
public class ScannerModuleItem extends ModItem {
    private final ScannerModule module;


    public ScannerModuleItem(final ScannerModule module) {
        this(module, new Properties());
    }

    public ScannerModuleItem(final ScannerModule module, final Properties properties) {
        super(properties);
        this.module = Objects.requireNonNull(module);
    }

    /** @return the scanner module implementation attached to this item */
    public ScannerModule getModule() {
        return module;
    }

    public int getEnergyCost(final ItemStack stack) {
        return module.getEnergyCost(stack);
    }
}
