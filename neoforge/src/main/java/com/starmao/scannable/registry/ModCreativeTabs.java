package com.starmao.scannable.registry;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Scannable.MOD_ID);

    public static final Supplier<CreativeModeTab> SCANNABLE = CREATIVE_TABS.register(com.starmao.scannable.Scannable.MOD_ID,
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.scannable_unofficial"))
                    .icon(() -> Items.SCANNER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(Items.SCANNER.get());
                        output.accept(Items.RANGE_MODULE.get());
                        output.accept(Items.FLUID_MODULE.get());
                        output.accept(Items.FRIENDLY_ENTITY_MODULE.get());
                        output.accept(Items.HOSTILE_ENTITY_MODULE.get());
                        output.accept(Items.BLOCK_MODULE.get());
                        output.accept(Items.ENTITY_MODULE.get());
                        output.accept(Items.ITEM_MODULE.get());
                        output.accept(Items.BLANK_MODULE.get());
                    }).build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }

    private ModCreativeTabs() {}
}
