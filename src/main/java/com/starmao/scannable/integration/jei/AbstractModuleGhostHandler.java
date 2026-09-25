package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.AbstractConfigurableScannerModuleContainerScreen;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base ghost ingredient handler for configurable module configuration screens.
 *
 * <p>Provides the common pattern of creating 5 configurable slot targets
 * from a JEI ingredient drag. Subclasses only need to implement
 * {@link #isValidIngredient} and {@link #getRegistryKey}.
 *
 * @param <S> the screen type (a subclass of {@link AbstractConfigurableScannerModuleContainerScreen})
 */
public abstract class AbstractModuleGhostHandler<S extends AbstractConfigurableScannerModuleContainerScreen<?, ?>>
        implements IGhostIngredientHandler<S> {

    /**
     * Validates that the dragged ingredient is applicable to this module type.
     *
     * @param stack the ingredient item stack
     * @return true if the ingredient can be configured into this module
     */
    protected abstract boolean isValidIngredient(ItemStack stack);

    /**
     * Resolves the registry key for the ingredient once it is dropped.
     *
     * @param stack the ingredient item stack
     * @return the registry key to send to the server, or empty if invalid
     */
    protected abstract Optional<ResourceLocation> getRegistryKey(ItemStack stack);

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            final S gui,
            final ITypedIngredient<I> ingredient,
            final boolean doStart) {

        if (!doStart) return List.of();
        if (!ServerConfig.HOOK_ALLOW_JEI.get()) return List.of();

        final Optional<ItemStack> stackOpt = ingredient.getItemStack();
        if (stackOpt.isEmpty()) return List.of();
        final ItemStack stack = stackOpt.get();
        if (!isValidIngredient(stack)) return List.of();

        final Optional<ResourceLocation> keyOpt = getRegistryKey(stack);
        if (keyOpt.isEmpty()) return List.of();
        final ResourceLocation key = keyOpt.get();

        final int guiLeft = gui.getGuiLeft();
        final int guiTop = gui.getGuiTop();
        final int originX = guiLeft + AbstractConfigurableScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int originY = guiTop + AbstractConfigurableScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = AbstractConfigurableScannerModuleContainerScreen.SLOT_SIZE;
        final int windowId = gui.getMenu().containerId;

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
                    Network.sendToServer(new SetConfiguredModuleItemAtMessage(
                            windowId, slotIndex, key));
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
