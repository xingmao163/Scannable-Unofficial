package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.capability.ScannerModuleCapability;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Utility for looking up a {@link ScannerModule} from any {@link ItemStack}.
 *
 * <p>Delegates to the {@link ScannerModuleCapability capability system} rather
 * than performing direct {@code instanceof} checks, making it compatible with
 * any item that exposes a ScannerModule through the capability (whether our
 * own module items or third-party additions).
 *
 * <p>This is the <strong>single access point</strong> for energy cost and
 * result provider queries; calling code should never reach into capability
 * internals directly.
 */
public final class ModuleHelper {
    public static Optional<ScannerModule> getModule(final ItemStack stack) {
        // Fast path for our own items, falls back to capability for addons.
        if (stack.getItem() instanceof ScannerModuleItem moduleItem) {
            return Optional.of(moduleItem.getModule());
        }
        return ScannerModuleCapability.get(stack);
    }

    public static int getEnergyCost(final ItemStack stack) {
        return getModule(stack).map(m -> m.getEnergyCost(stack)).orElse(0);
    }

    public static boolean hasResultProvider(final ItemStack stack) {
        return getModule(stack).map(ScannerModule::hasResultProvider).orElse(false);
    }

    private ModuleHelper() {}
}
