package com.starmao.scannable.common.container;

import com.starmao.scannable.common.item.ConfigurableBlockScannerModuleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the configurable block scanner module configuration screen.
 * <p>Handles adding/removing blocks from the module's target list via
 * {@link ConfigurableBlockScannerModuleItem}.
 */
public class BlockModuleContainerMenu extends AbstractModuleContainerMenu {
    public static BlockModuleContainerMenu create(int id, Inventory playerInventory, FriendlyByteBuf data) {
        InteractionHand hand = data.readEnum(InteractionHand.class);
        return new BlockModuleContainerMenu(id, playerInventory, hand);
    }

    public BlockModuleContainerMenu(int windowId, Inventory inventory, InteractionHand hand) {
        super(Containers.BLOCK_MODULE_CONTAINER.get(), windowId, inventory, hand);
    }

    @Override
    public void removeItemAt(int index) {
        ItemStack stack = getPlayer().getItemInHand(getHand());
        if (stack.getItem() instanceof ConfigurableBlockScannerModuleItem item) {
            item.removeValueAt(stack, index);
        }
    }

    @Override
    public void setItemAt(int index, Identifier name) {
        BuiltInRegistries.BLOCK.getOptional(name).ifPresent(block -> {
            ItemStack stack = getPlayer().getItemInHand(getHand());
            if (stack.getItem() instanceof ConfigurableBlockScannerModuleItem item) {
                item.setValueAt(stack, index, block);
            }
        });
    }
}
