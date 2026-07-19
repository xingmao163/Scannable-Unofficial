package com.starmao.scannable.registry;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registration of the mod's creative mode tab.
 * <p>Contains all scanner items and modules in a single tab group.
 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Scannable.MOD_ID);

    /** The main creative tab for Scannable Unofficial items. */
    public static final Supplier<CreativeModeTab> TAB = TABS.register("tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.scannable_unofficial"))
                    .icon(() -> new ItemStack(Items.SCANNER.get()))
                    .displayItems((params, output) -> {
                        output.accept(Items.SCANNER.get());
                        output.accept(Items.BLANK_MODULE.get());
                        output.accept(Items.RANGE_MODULE.get());
                        output.accept(Items.FLUID_MODULE.get());
                        output.accept(Items.BLOCK_MODULE.get());
                        output.accept(Items.ENTITY_MODULE.get());
                        output.accept(Items.FRIENDLY_ENTITY_MODULE.get());
                        output.accept(Items.HOSTILE_ENTITY_MODULE.get());
                        output.accept(Items.ITEM_MODULE.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    private ModCreativeTabs() {}
}
