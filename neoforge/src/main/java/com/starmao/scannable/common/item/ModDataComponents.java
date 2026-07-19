package com.starmao.scannable.common.item;

import com.mojang.serialization.Codec;
import com.starmao.scannable.Scannable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Registry for all custom data components used by scanner items.
 * <p>Each component stores a piece of persistent item data using Minecraft's
 * data component system (1.21+), with both persistent (NBT) and network
 * (buffer) codecs for survival and multiplayer support.
 *
 * <p>Components:
 * <ul>
 *   <li>{@link #SCANNER_ENERGY} — FE energy stored in the scanner</li>
 *   <li>{@link #SCANNER_MODULES} — module inventory contents</li>
 *   <li>{@link #ENTITY_TYPES} / {@link #BLOCKS} / {@link #SCAN_ITEMS} — configured target lists</li>
 *   <li>{@link #LOCKED} — whether a configurable module's target list is locked</li>
 * </ul>
 */
public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> REGISTER =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Scannable.MOD_ID);

    /** Energy (FE) stored in the scanner item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCANNER_ENERGY =
            REGISTER.register("energy", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** Module inventory contents (list of item stacks installed in the scanner). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SCANNER_MODULES =
            REGISTER.register("modules", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /** Configured entity type registry names for entity scanner modules. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> ENTITY_TYPES =
            REGISTER.register("entities", () -> DataComponentType.<List<ResourceLocation>>builder()
                    .persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /** Configured block registry names for block scanner modules. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> BLOCKS =
            REGISTER.register("blocks", () -> DataComponentType.<List<ResourceLocation>>builder()
                    .persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /** Configured item registry names for item scanner modules. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> SCAN_ITEMS =
            REGISTER.register("scan_items", () -> DataComponentType.<List<ResourceLocation>>builder()
                    .persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /** Whether a configurable module's target list is locked (prevents further changes). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LOCKED =
            REGISTER.register("locked", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** Last game tick when the charger module added energy. */
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
