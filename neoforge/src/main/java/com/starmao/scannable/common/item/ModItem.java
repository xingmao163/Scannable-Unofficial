package com.starmao.scannable.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;

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
        // In 26.1, item description IDs changed — simplified fallback
        Component name = stack.getHoverName();
        if (name != null) {
            MutableComponent description = Component.translatable(stack.getItem().getDescriptionId() + ".desc");
            tooltip.accept(description.withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
