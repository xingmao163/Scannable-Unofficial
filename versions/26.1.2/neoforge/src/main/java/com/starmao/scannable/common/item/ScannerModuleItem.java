package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.config.Strings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Item wrapper for a non-configurable scanner module.
 * <p>Each instance holds a reference to its {@link ScannerModule} implementation
 * (e.g. {@link com.starmao.scannable.common.scanning.RangeScannerModule}).
 * The module behaviour is accessed via {@link #getModule()}.
 */
public class ScannerModuleItem extends ModItem {
    private final ScannerModule module;

    public ScannerModuleItem(final ScannerModule module) {
        this(module, new Properties());
    }

    public ScannerModuleItem(final ScannerModule module, final Properties properties) {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        int cost = module.getEnergyCost(stack);
        if (cost > 0) {
            tooltip.accept(Strings.energyUsage(cost));
        }
    }
}
