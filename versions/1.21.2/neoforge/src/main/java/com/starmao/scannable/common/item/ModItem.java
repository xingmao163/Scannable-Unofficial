package com.starmao.scannable.common.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

/** Simple base item with a convenience factory for Item.Properties. */
public class ModItem extends Item {
    public ModItem(Properties properties) {
        super(properties);
    }

    public ModItem() {
        this(new Properties());
    }

    /**
     * Creates an {@link Item.Properties} with the given {@link ResourceLocation}
     * as its registry ID, then applies the supplied customizer.
     *
     * @param loc       the registry ID for this item
     * @param customizer a consumer that further configures the properties
     * @return a fully-configured {@link Item.Properties}
     */
    public static Item.Properties props(ResourceLocation loc, Consumer<Item.Properties> customizer) {
        Item.Properties properties = new Item.Properties();
        customizer.accept(properties);
        return properties;
    }
}
