package com.starmao.scannable.common.container;

import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the configurable item scanner module configuration screen.
 * <p>Handles adding/removing items from the module's target list via
 * {@link ConfigurableItemScannerModuleItem}.
 */
public class ItemModuleContainerMenu extends AbstractModuleContainerMenu {
    public static ItemModuleContainerMenu create(int id, Inventory playerInventory, FriendlyByteBuf data) {
        InteractionHand hand = data.readEnum(InteractionHand.class);
        return new ItemModuleContainerMenu(id, playerInventory, hand);
    }

    public ItemModuleContainerMenu(int windowId, Inventory inventory, InteractionHand hand) {
        super(Containers.ITEM_MODULE_CONTAINER.get(), windowId, inventory, hand);
    }


    @Override
    public void setItemAt(int index, Identifier name) {
        BuiltInRegistries.ITEM.getOptional(name).ifPresent(item -> {
            ItemStack stack = getPlayer().getItemInHand(getHand());
            if (stack.getItem() instanceof ConfigurableItemScannerModuleItem itemModule) {
                itemModule.setValueAt(stack, index, item);
            }
        });
    }
}
