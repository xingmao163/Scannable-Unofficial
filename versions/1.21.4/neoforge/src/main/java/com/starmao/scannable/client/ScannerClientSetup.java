package com.starmao.scannable.client;

import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ScannerContainerScreen;
import com.starmao.scannable.client.renderer.OverlayRenderer;
import com.starmao.scannable.client.renderer.ScannerRenderer;
import com.starmao.scannable.client.shader.Shaders;
import com.starmao.scannable.common.container.Containers;
import com.starmao.scannable.common.container.ModMenus;
import com.starmao.scannable.Scannable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ScannerClientSetup {
    public static void initialize(IEventBus modEventBus) {
        modEventBus.addListener(ScannerClientSetup::onClientSetup);
        modEventBus.addListener(ScannerClientSetup::registerScreens);
        modEventBus.addListener(ScannerClientSetup::registerGuiLayers);
        modEventBus.addListener(Shaders::onRegisterShaders);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.starmao.scannable.client.scanning.ScanResultProviders.initialize();

            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post evt) -> {
                ScanManager.tick();
            });

            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent evt) -> {
                if (evt.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                    ScannerRenderer.render(evt.getModelViewMatrix(), evt.getProjectionMatrix());
                    ScanManager.setMatrices(evt.getModelViewMatrix(), evt.getProjectionMatrix());
                    ScanManager.renderLevel(evt.getPartialTick().getGameTimeDeltaPartialTick(false));
                }
            });
        });
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SCANNER.get(), ScannerContainerScreen::new);
        event.register(Containers.BLOCK_MODULE_CONTAINER.get(), ConfigurableBlockScannerModuleContainerScreen::new);
        event.register(Containers.ENTITY_MODULE_CONTAINER.get(), ConfigurableEntityScannerModuleContainerScreen::new);
        event.register(Containers.ITEM_MODULE_CONTAINER.get(), ConfigurableItemScannerModuleContainerScreen::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Scannable.id("scanner_results"),
                (guiGraphics, deltaTracker) -> {
                    float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
                    ScanManager.renderGui(partialTick);
                    OverlayRenderer.render(guiGraphics, partialTick);
                });
    }

    private ScannerClientSetup() {
    }
}
