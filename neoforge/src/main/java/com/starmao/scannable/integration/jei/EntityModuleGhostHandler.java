package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI ghost-drag handler for the entity scanner module configuration screen.
 * <p>Allows players to drag entities (via spawn eggs) from JEI directly
 * into the config slots to add them to the module's target list.
 */
public class EntityModuleGhostHandler implements IGhostIngredientHandler<ConfigurableEntityScannerModuleContainerScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(final ConfigurableEntityScannerModuleContainerScreen gui, final I ingredient, final boolean start) {
        final List<Target<I>> targets = new ArrayList<>();
        final int slotsOriginX = ConfigurableEntityScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int slotsOriginY = ConfigurableEntityScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableEntityScannerModuleContainerScreen.SLOT_SIZE;

        if (ingredient instanceof ItemStack itemStack && itemStack.getItem() instanceof SpawnEggItem egg) {
            final EntityType<?> type = egg.getType(itemStack);
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
                        BuiltInRegistries.ENTITY_TYPE.getResourceKey(type).ifPresent(key ->
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
