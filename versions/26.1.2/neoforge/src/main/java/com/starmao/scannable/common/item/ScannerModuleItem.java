package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.config.Strings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;
import java.util.Objects;

public class ScannerModuleItem extends ModItem {
    private final ScannerModule module;

    public ScannerModuleItem(final ScannerModule module) {
        this.module = Objects.requireNonNull(module);
    }

    public ScannerModuleItem(Item.Properties properties, final ScannerModule module) {
        super(properties);
        this.module = Objects.requireNonNull(module);
    }
    public ScannerModule getModule() {
        return module;
    }

    public int getEnergyCost(final ItemStack stack) {
        return module.getEnergyCost(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        int cost = module.getEnergyCost(stack);
        if (cost > 0) {
            tooltip.accept(Strings.energyUsage(cost));
        }
    }
}
