package com.starmao.scannable.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Simple base item with description tooltip support.
 *
 * <p>Note: since 1.21.4, {@link Item.Properties} requires a registry id to be set
 * ({@code Item.Properties#setId}); an item constructed with bare properties throws
 * {@code NullPointerException: Item id not set}. Always build items through
 * {@code DeferredRegister.Items#registerItem}, which sets the id automatically and
 * passes the properties into the item constructor.
 */
public class ModItem extends Item {
    public ModItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tryAddDescription(stack, tooltip);
    }

    private static void tryAddDescription(ItemStack stack, List<Component> tooltip) {
        if (stack.isEmpty()) return;
        String translationKey = stack.getItem().getDescriptionId() + ".desc";
        Language language = Language.getInstance();
        if (language.has(translationKey)) {
            MutableComponent description = Component.translatable(translationKey);
            tooltip.add(description.withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
