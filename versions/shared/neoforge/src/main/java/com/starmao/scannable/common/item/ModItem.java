package com.starmao.scannable.common.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public class ModItem extends Item {
    protected static Properties props(ResourceLocation loc, Consumer<Properties> config) {
        Properties p = new Properties().setId(ResourceKey.create(Registries.ITEM, loc));
        if (config != null) config.accept(p);
        return p;
    }

    public ModItem(Properties properties) {
        super(properties);
    }

    public ModItem() {
        this(new Properties());
    }
}
