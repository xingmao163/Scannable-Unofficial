package com.starmao.scannable.common.capability;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.item.ScannerModuleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.Optional;

/**
 * Central definition and registration for the {@link ScannerModule} capability.
 *
 * <p>This capability is attached to every scanner module item, allowing
 * uniform access to a module's scanning behaviour (energy cost, scan logic,
 * result providers) via the NeoForge capability system rather than brittle
 * {@code instanceof} checks.
 *
 * <p>Usage:
 * <pre>{@code
 * ScannerModule module = stack.getCapability(ScannerModuleCapability.CAPABILITY);
 * }</pre>
 */
public final class ScannerModuleCapability {

    /**
     * The capability key. {@code Void} context because a module's behaviour
     * is intrinsic to the item stack and doesn't vary by neighbour block
     * or side.
     */
    public static final ItemCapability<ScannerModule, Void> CAPABILITY =
            ItemCapability.createVoid(
                    Scannable.id("scanner_module"),
                    ScannerModule.class);

    private ScannerModuleCapability() {
    }

    /**
     * Register the capability on every module item.
     * <p>
     * Call this from a {@link RegisterCapabilitiesEvent} handler.
     */
    public static void register(final RegisterCapabilitiesEvent event,
                                final ScannerModuleItem... moduleItems) {
        event.registerItem(CAPABILITY,
                ScannerModuleCapability::getModule,
                moduleItems);
    }

    /**
     * Convenience lookup — returns the {@link ScannerModule} for a stack
     * if the item holds one, without worrying about null capability returns.
     */
    public static Optional<ScannerModule> get(final ItemStack stack) {
        return Optional.ofNullable(stack.getCapability(CAPABILITY));
    }

    // -- provider -- //

    private static ScannerModule getModule(final ItemStack stack, final Void ctx) {
        if (stack.getItem() instanceof final ScannerModuleItem moduleItem) {
            return moduleItem.getModule();
        }
        return null;
    }
}
