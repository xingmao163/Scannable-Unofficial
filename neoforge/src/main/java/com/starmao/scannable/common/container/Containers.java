package com.starmao.scannable.common.container;

import com.starmao.scannable.Scannable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Containers {
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(Registries.MENU, Scannable.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BlockModuleContainerMenu>> BLOCK_MODULE_CONTAINER =
            CONTAINERS.register("block_module", () -> IMenuTypeExtension.create(BlockModuleContainerMenu::create));
    public static final DeferredHolder<MenuType<?>, MenuType<EntityModuleContainerMenu>> ENTITY_MODULE_CONTAINER =
            CONTAINERS.register("entity_module", () -> IMenuTypeExtension.create(EntityModuleContainerMenu::create));
    public static final DeferredHolder<MenuType<?>, MenuType<ItemModuleContainerMenu>> ITEM_MODULE_CONTAINER =
            CONTAINERS.register("item_module", () -> IMenuTypeExtension.create(ItemModuleContainerMenu::create));

    public static void register(IEventBus modEventBus) {
        CONTAINERS.register(modEventBus);
    }

    private Containers() {
    }
}
