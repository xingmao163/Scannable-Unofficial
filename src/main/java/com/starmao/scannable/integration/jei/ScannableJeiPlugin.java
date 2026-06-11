package com.starmao.scannable.integration.jei;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration plugin for Scannable-Unofficial.
 *
 * <p>Registers a ghost ingredient handler for the configurable module screens,
 * allowing players to drag items from JEI to configure block scanner modules.
 */
@JeiPlugin
public class ScannableJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Scannable.id("jei_plugin");
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
                ConfigurableBlockScannerModuleContainerScreen.class,
                new BlockModuleGhostHandler());
        registration.addGhostIngredientHandler(
                ConfigurableEntityScannerModuleContainerScreen.class,
                new EntityModuleGhostHandler());
        registration.addGhostIngredientHandler(
                ConfigurableItemScannerModuleContainerScreen.class,
                new ItemModuleGhostHandler());
    }
}
