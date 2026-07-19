package com.starmao.scannable.common.item;

import com.mojang.serialization.Codec;
import com.starmao.scannable.Scannable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> REGISTER =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Scannable.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCANNER_ENERGY =
            REGISTER.register("energy", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SCANNER_MODULES =
            REGISTER.register("modules", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Identifier>>> ENTITY_TYPES =
            REGISTER.register("entities", () -> DataComponentType.<List<Identifier>>builder()
                    .persistent(Identifier.CODEC.listOf())
                    .networkSynchronized(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Identifier>>> BLOCKS =
            REGISTER.register("blocks", () -> DataComponentType.<List<Identifier>>builder()
                    .persistent(Identifier.CODEC.listOf())
                    .networkSynchronized(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Identifier>>> SCAN_ITEMS =
            REGISTER.register("scan_items", () -> DataComponentType.<List<Identifier>>builder()
                    .persistent(Identifier.CODEC.listOf())
                    .networkSynchronized(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LOCKED =
            REGISTER.register("locked", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_CHARGE_TICK =
            REGISTER.register("last_charge_tick", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    private ModDataComponents() {}
}
