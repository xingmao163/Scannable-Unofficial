package com.starmao.scannable.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Simple base item with description tooltip support. */
public class ModItem extends Item {
    public ModItem(Properties properties) {
        super(properties);
    }

    public ModItem() {
        this(new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tryAddDescription(stack, tooltip);
    }

    private static void tryAddDescription(ItemStack stack, Consumer<Component> tooltip) {
        if (stack.isEmpty()) return;
        String translationKey = stack.getItem().getDescriptionId() + ".desc";
        Language language = Language.getInstance();
        if (language.has(translationKey)) {
            tooltip.accept(Component.translatable(translationKey).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
