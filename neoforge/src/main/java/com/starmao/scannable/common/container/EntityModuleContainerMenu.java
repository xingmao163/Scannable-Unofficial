package com.starmao.scannable.common.container;

import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the configurable entity scanner module configuration screen.
 * <p>Handles adding/removing entity types from the module's target list via
 * {@link ConfigurableEntityScannerModuleItem}.
 */
public class EntityModuleContainerMenu extends AbstractModuleContainerMenu {
    public static EntityModuleContainerMenu create(int windowId, Inventory inventory, FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new EntityModuleContainerMenu(windowId, inventory, hand);
    }

    public EntityModuleContainerMenu(int windowId, Inventory inventory, InteractionHand hand) {
        super(Containers.ENTITY_MODULE_CONTAINER.get(), windowId, inventory, hand);
    }

    protected EntityModuleContainerMenu(MenuType<?> type, int windowId, Inventory inventory, InteractionHand hand) {
        super(type, windowId, inventory, hand);
    }


    @Override
    public void setItemAt(int index, ResourceLocation name) {
        ItemStack stack = getPlayer().getItemInHand(getHand());
        BuiltInRegistries.ENTITY_TYPE.getOptional(name).ifPresent(type -> {
            if (stack.getItem() instanceof ConfigurableEntityScannerModuleItem item) {
                item.setValueAt(stack, index, type);
            }
        });
    }
}
