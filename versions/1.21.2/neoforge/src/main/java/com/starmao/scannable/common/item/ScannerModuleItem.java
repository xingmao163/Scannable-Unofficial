package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.config.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ScannerModuleItem extends ModItem {
    private final ScannerModule module;

    public ScannerModuleItem(Item.Properties properties, ScannerModule module) {
        super(properties);
        this.module = module;
    }

    public ScannerModule getModule() {
        return module;
    }

    public int getModuleEnergyCost() {
        return module.getEnergyCost(ItemStack.EMPTY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        Component desc = getModuleDescription();
        if (desc != null) {
            tooltip.add(desc);
        }

        int cost = getModuleEnergyCost();
        if (cost > 0) {
            tooltip.add(Strings.energyUsage(cost));
        }
    }

    private Component getModuleDescription() {
        String key = getDescriptionId() + ".desc";
        if (Component.translatable(key).getString().equals(key)) {
            return null;
        }
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }
}
