package com.starmao.scannable.common.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ItemTags {
    private static final String MOD_ID = "scannable_unofficial";

    public static final TagKey<Item> MODULES = tag("modules");

    public static void initialize() {
    }

    private static TagKey<Item> tag(final String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
    }
}
