package com.starmao.scannable.integration.jei;

import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

/**
 * Ghost ingredient handler for the configurable block scanner module screen.
 *
 * <p>Allows players to drag block items from JEI's ingredient panel directly onto
 * the module's configuration slots to add them as scan targets.
 */
public class BlockModuleGhostHandler extends AbstractModuleGhostHandler<ConfigurableBlockScannerModuleContainerScreen> {

    @Override
    protected boolean isValidIngredient(final ItemStack stack) {
        return Block.byItem(stack.getItem()) != Blocks.AIR;
    }

    @Override
    protected Optional<ResourceLocation> getRegistryKey(final ItemStack stack) {
        final Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return Optional.empty();
        return BuiltInRegistries.BLOCK.getResourceKey(block)
                .map(ResourceKey::location);
    }
}
