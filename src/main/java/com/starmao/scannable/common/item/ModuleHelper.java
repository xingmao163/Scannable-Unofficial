package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Utility for looking up a {@link ScannerModule} from any {@link ItemStack}.
 *
 * <p>Works with both direct {@link ScannerModuleItem} instances and
 * capability-based module lookups, providing a single access point for
 * energy cost and result provider queries.
 */
public final class ModuleHelper {
    public static Optional<ScannerModule> getModule(final ItemStack stack) {
        if (stack.getItem() instanceof ScannerModuleItem moduleItem) {
            return Optional.of(moduleItem.getModule());
        }
        return Optional.empty();
    }

    public static int getEnergyCost(final ItemStack stack) {
        return getModule(stack).map(m -> m.getEnergyCost(stack)).orElse(0);
    }

    public static boolean hasResultProvider(final ItemStack stack) {
        return getModule(stack).map(ScannerModule::hasResultProvider).orElse(false);
    }

    private ModuleHelper() {}
}
