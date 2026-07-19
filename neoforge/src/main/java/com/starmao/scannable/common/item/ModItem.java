package com.starmao.scannable.common.item;

import net.minecraft.world.item.Item;

/** Simple base item. */
public class ModItem extends Item {
    public ModItem(Properties properties) {
        super(properties);
    }

    public ModItem() {
        this(new Properties());
    }
}
