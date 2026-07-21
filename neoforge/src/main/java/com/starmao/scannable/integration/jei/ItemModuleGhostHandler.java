package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ghost ingredient handler for the configurable item scanner module screen.
 *
 * <p>Allows players to drag any item from JEI's ingredient panel directly onto
 * the module's configuration slots to add them as scan targets.
 */
public class ItemModuleGhostHandler implements IGhostIngredientHandler<ConfigurableItemScannerModuleContainerScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            final ConfigurableItemScannerModuleContainerScreen gui,
            final ITypedIngredient<I> ingredient,
            final boolean doStart) {

        if (!doStart) {
            return List.of();
        }

        if (!ServerConfig.HOOK_ALLOW_JEI.get()) return List.of();

        final Optional<ItemStack> itemStackOpt = ingredient.getItemStack();
        if (itemStackOpt.isEmpty() || itemStackOpt.get().isEmpty()) {
            return List.of();
        }

        final ItemStack itemStack = itemStackOpt.get();

        // Calculate screen coordinates for the 5 configuration slots
        final int guiLeft = gui.getGuiLeft();
        final int guiTop = gui.getGuiTop();
        final int originX = guiLeft + ConfigurableItemScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int originY = guiTop + ConfigurableItemScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableItemScannerModuleContainerScreen.SLOT_SIZE;

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
                    BuiltInRegistries.ITEM.getResourceKey(itemStack.getItem()).ifPresent(key ->
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
