package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ghost ingredient handler for the configurable block scanner module screen.
 *
 * <p>Allows players to drag block items from JEI's ingredient panel directly onto
 * the module's configuration slots to add them as scan targets.
 */
public class BlockModuleGhostHandler implements IGhostIngredientHandler<ConfigurableBlockScannerModuleContainerScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            final ConfigurableBlockScannerModuleContainerScreen gui,
            final ITypedIngredient<I> ingredient,
            final boolean doStart) {

        // Only show targets when the player has actually picked up the ingredient
        if (!doStart) {
            return List.of();
        }

        // Extract the ingredient as an ItemStack
        final Optional<ItemStack> itemStackOpt = ingredient.getItemStack();
        if (itemStackOpt.isEmpty()) {
            return List.of();
        }

        final ItemStack itemStack = itemStackOpt.get();
        final Block block = Block.byItem(itemStack.getItem());
        if (block == Blocks.AIR) {
            return List.of();
        }

        // Calculate screen coordinates for the 5 configuration slots
        final int guiLeft = gui.getGuiLeft();
        final int guiTop = gui.getGuiTop();
        final int originX = guiLeft + ConfigurableBlockScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int originY = guiTop + ConfigurableBlockScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableBlockScannerModuleContainerScreen.SLOT_SIZE;

        final List<Target<I>> targets = new ArrayList<>(5);
        for (int slot = 0; slot < 5; slot++) {
            final int slotIndex = slot;
            final int slotX = originX + slot * slotSize;
            final Rect2i area = new Rect2i(slotX, originY, slotSize, slotSize);

            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(final I ingredient) {
                    // Map the ItemStack back to a Block and send the configure packet
                    final Block targetBlock = Block.byItem(itemStack.getItem());
                    if (targetBlock == Blocks.AIR) return;

                    BuiltInRegistries.BLOCK.getResourceKey(targetBlock).ifPresent(key ->
                            Network.sendToServer(new SetConfiguredModuleItemAtMessage(
                                    gui.getMenu().containerId,
                                    slotIndex,
                                    key.location())));
                }
            });
        }

        return targets;
    }

    @Override
    public void onComplete() {
        // No cleanup needed
    }
}
