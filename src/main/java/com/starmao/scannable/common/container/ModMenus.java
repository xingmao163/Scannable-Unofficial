package com.starmao.scannable.common.container;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.container.ScannerContainerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Scannable.MOD_ID);

    public static final Supplier<MenuType<ScannerContainerMenu>> SCANNER = MENUS.register("scanner",
            () -> IMenuTypeExtension.create(ScannerContainerMenu::create));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {}
}
