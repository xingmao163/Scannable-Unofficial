package com.starmao.scannable.common.container;

import com.starmao.scannable.Scannable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registry for the main scanner container menu type.
 * <p>Separate from {@link Containers} because the scanner menu is registered
 * under "scannable_unofficial:scanner" while configurable module menus are
 * registered under their own names.
 */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Scannable.MOD_ID);

    /** Menu type for the main scanner inventory GUI (active/inactive module slots). */
    public static final Supplier<MenuType<ScannerContainerMenu>> SCANNER = MENUS.register("scanner",
            () -> IMenuTypeExtension.create(ScannerContainerMenu::create));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {}
}
