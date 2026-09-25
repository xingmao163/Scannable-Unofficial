package com.starmao.scannable.common.container;

import com.starmao.scannable.Scannable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry of custom container menu types for configurable scanner modules.
 * <p>Each entry defines a {@link MenuType} used to open the configuration
 * screen for a specific module type (blocks, entities, items).
 */
public final class Containers {
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(Registries.MENU, Scannable.MOD_ID);

    /** Menu type for the block module configuration screen. */
    public static final DeferredHolder<MenuType<?>, MenuType<BlockModuleContainerMenu>> BLOCK_MODULE_CONTAINER =
            CONTAINERS.register("block_module", () -> IMenuTypeExtension.create(BlockModuleContainerMenu::create));
    /** Menu type for the entity module configuration screen. */
    public static final DeferredHolder<MenuType<?>, MenuType<EntityModuleContainerMenu>> ENTITY_MODULE_CONTAINER =
            CONTAINERS.register("entity_module", () -> IMenuTypeExtension.create(EntityModuleContainerMenu::create));
    /** Menu type for the item module configuration screen. */
    public static final DeferredHolder<MenuType<?>, MenuType<ItemModuleContainerMenu>> ITEM_MODULE_CONTAINER =
            CONTAINERS.register("item_module", () -> IMenuTypeExtension.create(ItemModuleContainerMenu::create));

    public static void register(IEventBus modEventBus) {
        CONTAINERS.register(modEventBus);
    }

    private Containers() {
    }
}
