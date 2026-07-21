package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.Optional;

/**
 * Ghost ingredient handler for the configurable entity scanner module screen.
 *
 * <p>Allows players to drag spawn eggs from JEI's ingredient panel directly onto
 * the module's configuration slots to add them as scan targets.
 */
public class EntityModuleGhostHandler extends AbstractModuleGhostHandler<ConfigurableEntityScannerModuleContainerScreen> {

    @Override
    protected boolean isValidIngredient(final ItemStack stack) {
        if (!(stack.getItem() instanceof SpawnEggItem egg)) return false;
        final var entityType = egg.getType(stack);
        return !BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
                .equals(BuiltInRegistries.ENTITY_TYPE.getDefaultKey());
    }

    @Override
    protected Optional<ResourceLocation> getRegistryKey(final ItemStack stack) {
        final SpawnEggItem egg = (SpawnEggItem) stack.getItem();
        return BuiltInRegistries.ENTITY_TYPE.getResourceKey(egg.getType(stack))
                .map(ResourceKey::location);
    }
}
