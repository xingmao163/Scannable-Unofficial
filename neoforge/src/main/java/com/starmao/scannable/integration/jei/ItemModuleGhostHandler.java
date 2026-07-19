package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI ghost-drag handler for the item scanner module configuration screen.
 * <p>Allows players to drag items from JEI directly into the config slots
 * to add them to the module's scan target list.
 */
public class ItemModuleGhostHandler implements IGhostIngredientHandler<ConfigurableItemScannerModuleContainerScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(final ConfigurableItemScannerModuleContainerScreen gui, final I ingredient, final boolean start) {
        final List<Target<I>> targets = new ArrayList<>();
        final int slotsOriginX = ConfigurableItemScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int slotsOriginY = ConfigurableItemScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableItemScannerModuleContainerScreen.SLOT_SIZE;

        if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
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
                        BuiltInRegistries.ITEM.getResourceKey(stack.getItem()).ifPresent(key ->
                                Network.sendToServer(new SetConfiguredModuleItemAtMessage(
                                        gui.getMenu().containerId, slotIndex, key.location())));
                    }
                });
            }
        }
        return targets;
    }

    @Override
    public void onComplete() {}
}
