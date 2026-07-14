package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityModuleGhostHandler implements IGhostIngredientHandler<ConfigurableEntityScannerModuleContainerScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            final ConfigurableEntityScannerModuleContainerScreen gui,
            final ITypedIngredient<I> ingredient,
            final boolean doStart) {

        if (!doStart) {
            return List.of();
        }

        final Optional<ItemStack> itemStackOpt = ingredient.getItemStack();
        if (itemStackOpt.isEmpty()) {
            return List.of();
        }

        final ItemStack itemStack = itemStackOpt.get();
        if (!(itemStack.getItem() instanceof SpawnEggItem)) {
            return List.of();
        }

        final SpawnEggItem egg = (SpawnEggItem) itemStack.getItem();
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        final var entityType = egg.getType(level.registryAccess(), itemStack);
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(BuiltInRegistries.ENTITY_TYPE.getDefaultKey())) {
            return List.of();
        }

        final int guiLeft = gui.getGuiLeft();
        final int guiTop = gui.getGuiTop();
        final int originX = guiLeft + ConfigurableEntityScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int originY = guiTop + ConfigurableEntityScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableEntityScannerModuleContainerScreen.SLOT_SIZE;

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
                    var level = Minecraft.getInstance().level;
                    if (level == null) return;
                    final SpawnEggItem egg = (SpawnEggItem) itemStack.getItem();
                    final var entityType = egg.getType(level.registryAccess(), itemStack);

                    BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).ifPresent(key ->
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
    }
}
