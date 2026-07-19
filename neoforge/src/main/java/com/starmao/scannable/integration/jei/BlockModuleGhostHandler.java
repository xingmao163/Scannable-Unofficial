package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.common.item.ConfigurableBlockScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI ghost-drag handler for the block scanner module configuration screen.
 * <p>Allows players to drag blocks from JEI directly into the config slots
 * to add them to the module's target list.
 */
public class BlockModuleGhostHandler implements IGhostIngredientHandler<ConfigurableBlockScannerModuleContainerScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(final ConfigurableBlockScannerModuleContainerScreen gui, final I ingredient, final boolean start) {
        final List<Target<I>> targets = new ArrayList<>();
        // Calculate slot positions (matching the 5 slots in the config GUI)
        final int slotsOriginX = ConfigurableBlockScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int slotsOriginY = ConfigurableBlockScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableBlockScannerModuleContainerScreen.SLOT_SIZE;

        if (ingredient instanceof ItemStack itemStack && itemStack.getItem() instanceof BlockItem) {
            for (int i = 0; i < com.starmao.scannable.common.config.Constants.CONFIGURABLE_MODULE_SLOTS; i++) {
                final int slotIndex = i;
                final int x = slotsOriginX + slotIndex * slotSize;
                final int y = slotsOriginY;
                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() {
                        return new Rect2i(x, y, slotSize, slotSize);
                    }

                    @Override
                    public void accept(final I ingredient) {
                        if (ingredient instanceof ItemStack stack) {
                            final Block block = Block.byItem(stack.getItem());
                            if (block != net.minecraft.world.level.block.Blocks.AIR) {
                                BuiltInRegistries.BLOCK.getResourceKey(block).ifPresent(key ->
                                        Network.sendToServer(new SetConfiguredModuleItemAtMessage(
                                                gui.getMenu().containerId, slotIndex, key.location())));
                            }
                        }
                    }
                });
            }
        }
        return targets;
    }

    @Override
    public void onComplete() {}
}
