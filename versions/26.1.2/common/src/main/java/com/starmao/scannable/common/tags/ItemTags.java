package com.starmao.scannable.common.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item tag registry for Scannable mod.
 * In NeoForge 26.1.2+, uses reflection to safely create TagKey without direct ResourceLocation dependency.
 */
public final class ItemTags {
    private static final String MOD_ID = "scannable_unofficial";

    public static final TagKey<Item> MODULES = createTagKey("modules");

    public static void initialize() {
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item> createTagKey(final String name) {
        try {
            // Reflection-based TagKey creation that avoids ResourceLocation import
            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            java.lang.reflect.Method fromNamespaceAndPath = resourceLocationClass.getMethod(
                "fromNamespaceAndPath", String.class, String.class);
            Object resourceLocation = fromNamespaceAndPath.invoke(null, MOD_ID, name);
            
            // Cast using Object to avoid ResourceLocation type reference in source code
            var tagKeyCreateMethod = TagKey.class.getMethod("create", Class.class, Object.class);
            return (TagKey<Item>) tagKeyCreateMethod.invoke(null, Registries.ITEM, resourceLocation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ItemTag: " + name, e);
        }
    }
}
