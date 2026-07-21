package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Ghost ingredient handler for the configurable item scanner module screen.
 *
 * <p>Allows players to drag any item from JEI's ingredient panel directly onto
 * the module's configuration slots to add them as scan targets.
 */
public class ItemModuleGhostHandler extends AbstractModuleGhostHandler<ConfigurableItemScannerModuleContainerScreen> {

    @Override
    protected boolean isValidIngredient(final ItemStack stack) {
        return !stack.isEmpty();
    }

    @Override
    protected Optional<ResourceLocation> getRegistryKey(final ItemStack stack) {
        return BuiltInRegistries.ITEM.getResourceKey(stack.getItem())
                .map(ResourceKey::location);
    }
}
