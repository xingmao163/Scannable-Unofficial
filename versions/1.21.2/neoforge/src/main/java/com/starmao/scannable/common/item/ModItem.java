package com.starmao.scannable.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

/** Simple base item with description tooltip support and a convenience factory for Item.Properties. */
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
        String translationKey = stack.getItem().getDescriptionId() + ".desc";
        Language language = Language.getInstance();
        if (language.has(translationKey)) {
            MutableComponent description = Component.translatable(translationKey);
            tooltip.add(description.withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Creates an {@link Item.Properties} with the given {@link ResourceLocation}
     * as its registry ID, then applies the supplied customizer.
     *
     * @param loc       the registry ID for this item
     * @param customizer a consumer that further configures the properties
     * @return a fully-configured {@link Item.Properties}
     */
    public static Item.Properties props(ResourceLocation loc, Consumer<Item.Properties> customizer) {
        Item.Properties properties = new Item.Properties();
        customizer.accept(properties);
        return properties;
    }
}
