package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.capability.ScannerModuleCapability;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Convenience helper for accessing {@link ScannerModule} instances from item stacks.
 * <p>Routes lookups through {@link ScannerModuleCapability#get(ItemStack)} so callers
 * never deal with the capability system directly.
 */
public final class ModuleHelper {

    /**
     * Returns the {@link ScannerModule} for the given item stack, if the item
     * exposes the scanner module capability.
     *
     * @param stack the item stack to query
     * @return the module, or empty if the item is not a scanner module
     */
    public static Optional<ScannerModule> getModule(final ItemStack stack) {
        return ScannerModuleCapability.get(stack);
    }

    private ModuleHelper() {}
}
