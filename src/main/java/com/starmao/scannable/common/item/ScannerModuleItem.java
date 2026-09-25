package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import net.minecraft.world.item.Item;

import com.starmao.scannable.common.config.Strings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Objects;

/**
 * Item wrapper for a non-configurable scanner module.
 * <p>1.21.2 variant — takes explicit {@link Item.Properties} so the
 * registry name / descriptionId can be set before the Item constructor
 * runs (vanilla 1.21.2+ requires descriptionId on Properties).
 */
public class ScannerModuleItem extends ModItem {
    private final ScannerModule module;

    public ScannerModuleItem(final ScannerModule module) {
        this(module, new Item.Properties());
    }

    public ScannerModuleItem(final ScannerModule module, final Item.Properties properties) {
        super(properties);
        this.module = Objects.requireNonNull(module);
    }

    /** @return the scanner module implementation attached to this item */
    public ScannerModule getModule() {
        return module;
    }

    public int getEnergyCost(final ItemStack stack) {
        return module.getEnergyCost(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int cost = module.getEnergyCost(stack);
        if (cost > 0) {
            tooltip.add(Strings.energyUsage(cost));
        }
    }
}
