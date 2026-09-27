package com.starmao.scannable.integration.jei;

import com.starmao.scannable.Scannable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

/**
 * JEI (Just Enough Items) integration plugin for Scannable Unofficial.
 * <p>Registers ghost-drag handlers for configurable module screens so that
 * players can drag items/entities/blocks from JEI directly into the
 * configuration slots.
 */
@JeiPlugin
public final class ScannableJeiPlugin implements IModPlugin {
    private static final Identifier UID = Scannable.id("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        // No custom recipes to register — module configuration uses ghost drag only.
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
                com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen.class,
                new BlockModuleGhostHandler());
        registration.addGhostIngredientHandler(
                com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen.class,
                new EntityModuleGhostHandler());
        registration.addGhostIngredientHandler(
                com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen.class,
                new ItemModuleGhostHandler());
    }
}
