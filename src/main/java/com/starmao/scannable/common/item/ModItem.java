package com.starmao.scannable.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Simple base item with description tooltip support. */
public class ModItem extends Item {
    public ModItem(Properties properties) {
        super(properties);
    }

    public ModItem() {
        this(new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tryAddDescription(stack, tooltip);
    }

    private static void tryAddDescription(ItemStack stack, List<Component> tooltip) {
        if (stack.isEmpty()) return;
        String translationKey = stack.getDescriptionId() + ".desc";
        Language language = Language.getInstance();
        if (language.has(translationKey)) {
            MutableComponent description = Component.translatable(translationKey);
            tooltip.add(description.withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
