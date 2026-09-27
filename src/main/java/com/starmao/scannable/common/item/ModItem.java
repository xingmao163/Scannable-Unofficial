package com.starmao.scannable.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

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
        final String descKey = stack.getItem().getDescriptionId() + ".desc";
        // Only show the description line when a translation actually exists for the key.
        // Without this check every item lacking a ".desc" entry would render the raw
        // translation key in its tooltip.
        if (!Language.getInstance().has(descKey)) return;
        MutableComponent description = Component.translatable(descKey);
        tooltip.accept(description.withStyle(ChatFormatting.DARK_GRAY));
    }
}
