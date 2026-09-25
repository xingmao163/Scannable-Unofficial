package com.starmao.scannable.registry;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.item.ModItem;
import com.starmao.scannable.common.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registration of the mod's creative mode tab.
 * <p>All {@link ModItem} subclasses are automatically discovered via
 * {@link BuiltInRegistries#ITEM} — no manual item list to maintain.
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
                        // All ModItem subclasses, automatically discovered
                        BuiltInRegistries.ITEM.stream()
                                .filter(item -> Scannable.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()))
                                .forEach(item -> output.accept(new ItemStack(item)));
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    private ModCreativeTabs() {}
}
