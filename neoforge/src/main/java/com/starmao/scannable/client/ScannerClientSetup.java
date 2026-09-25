package com.starmao.scannable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.client.ClientScanHandlerImpl;
import com.starmao.scannable.Scannable;
import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ScannerContainerScreen;
import com.starmao.scannable.client.renderer.OverlayRenderer;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import com.starmao.scannable.client.renderer.ScannerRenderer;
import com.starmao.scannable.client.scanning.ScanResultProviders;
import com.starmao.scannable.common.container.Containers;
import com.starmao.scannable.common.container.ModMenus;
import com.starmao.scannable.common.util.ClientAccessor;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side initialisation for scanner rendering, audio, and GUI on 26.1.2.
 * <p>
 * Registers custom {@link net.minecraft.client.renderer.rendertype.RenderType} pipelines,
 * hooks world and GUI rendering, and wires scan lifecycle events.
 */
public final class ScannerClientSetup {
    public static void initialize(IEventBus modEventBus) {
        // Register the client-only scan handler so common ScannerItem can
        // call it without direct class references (safe on dedicated server).
        ClientAccessor.setHandler(new ClientScanHandlerImpl());

        modEventBus.addListener(ScannerClientSetup::onClientSetup);
        modEventBus.addListener(ScannerClientSetup::registerScreens);
        modEventBus.addListener(ScannerClientSetup::registerGuiLayers);
        modEventBus.addListener(ScannerClientSetup::handleRegisterRenderPipelines);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialise scan result providers (block, entity, item scanners).
            ScanResultProviders.initialize();

            // Tick the scan lifecycle every client tick.
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post evt) -> {
                ScanManager.tick();
            });

            // Render scan wave + result boxes in the world after the level is complete.
            // The scan wave is a full-screen shader quad that samples the depth buffer;
            // it must render after the entire scene is drawn (AFTER_LEVEL) to composite
            // correctly on top of all world content.
            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterLevel event1) -> {
                float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                PoseStack poseStack = new PoseStack();
                poseStack.last().pose().set(event1.getModelViewMatrix());
                ScanManager.renderLevel(poseStack, partialTick);
                ScannerRenderer.INSTANCE.render(event1.getModelViewMatrix());
            });
        });
    }

    private static void handleRegisterRenderPipelines(final RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ScanResultRenderType.SCAN_EFFECT_PIPELINE);
        event.registerPipeline(ScanResultRenderType.RESULT_BOX_PIPELINE);
        event.registerPipeline(ScanResultRenderType.SHIMMER_PIPELINE);
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

    private ScannerClientSetup() {}
}
