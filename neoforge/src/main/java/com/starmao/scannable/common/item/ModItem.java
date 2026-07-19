package com.starmao.scannable.common.item;

import net.minecraft.world.item.Item;

/**
 * Simple base item with default scanner-mod settings.
 * <p>Used as the superclass for most scanner items and as a standalone
 * item class for the blank module.
 */
public class ModItem extends Item {
    public ModItem() {
        super(new Properties());
    }
}
