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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import com.starmao.scannable.client.config.ClientConfig;
import net.neoforged.bus.api.SubscribeEvent;
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
@EventBusSubscriber(modid = Scannable.MOD_ID, value = Dist.CLIENT)
public final class ScannerClientSetup {
    

        @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register the client-only scan handler so common ScannerItem can
        // call it without direct class references (safe on dedicated server).
        ClientAccessor.setHandler(new ClientScanHandlerImpl());

        // Make the Config button clickable in the Mods screen.
        ModList.get().getModContainerById(Scannable.MOD_ID).ifPresent(modContainer -> {
            ClientRegistrations.registerConfigScreen(modContainer);
        });


        event.enqueueWork(() -> {
            // Initialise scan result providers (block, entity, item scanners).
            ScanResultProviders.initialize();

            // Tick the scan lifecycle every client tick.
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post evt) -> {
                ScanManager.tick();
            });

            // Render scan wave + result boxes in the world after the level is complete.
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent event1) -> {
                ScanManager.renderLevel(event1);
            });

            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterLevel event1) -> {
                ScannerRenderer.INSTANCE.render(event1.getModelViewMatrix(), event1.getLevelRenderState().cameraRenderState);
            });
        });
    }

        @SubscribeEvent
    public static void handleRegisterRenderPipelines(final RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ScanResultRenderType.SCAN_EFFECT_PIPELINE);
        event.registerPipeline(ScanResultRenderType.RESULT_BOX_PIPELINE);
        event.registerPipeline(ScanResultRenderType.SHIMMER_PIPELINE);
        event.registerPipeline(ScanResultRenderType.SCAN_PROGRESS_PIPELINE);
    }

        @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SCANNER.get(), ScannerContainerScreen::new);
        event.register(Containers.BLOCK_MODULE_CONTAINER.get(), ConfigurableBlockScannerModuleContainerScreen::new);
        event.register(Containers.ENTITY_MODULE_CONTAINER.get(), ConfigurableEntityScannerModuleContainerScreen::new);
        event.register(Containers.ITEM_MODULE_CONTAINER.get(), ConfigurableItemScannerModuleContainerScreen::new);
    }

        @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Scannable.id("scanner_results"),
                (guiGraphics, deltaTracker) -> {
                    float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
                    ScanManager.renderGui(partialTick);
                    OverlayRenderer.render(guiGraphics, partialTick);
                });
    }

        @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == ClientConfig.SPEC) {
            ClientConfig.clearCache();
            com.starmao.scannable.client.scanning.ProviderCacheManager.clearCache();
        }
    }

    private ScannerClientSetup() {}
}
