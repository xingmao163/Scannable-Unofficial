package com.starmao.scannable.client;

import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ScannerContainerScreen;
import com.starmao.scannable.client.renderer.OverlayRenderer;
import com.starmao.scannable.client.renderer.ScannerRenderer;
import com.starmao.scannable.client.renderer.ScanResultRenderType;
import com.starmao.scannable.client.shader.Shaders;
import com.starmao.scannable.common.container.Containers;
import com.starmao.scannable.common.container.ModMenus;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-side initialisation for scanner rendering, audio, and GUI. */
public final class ScannerClientSetup {
    public static void initialize(IEventBus modEventBus) {
        modEventBus.addListener(ScannerClientSetup::onClientSetup);
        modEventBus.addListener(ScannerClientSetup::registerScreens);
        modEventBus.addListener(ScannerClientSetup::onRegisterPipelines);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Shaders.initialize();
            // Initialize provider-based scanning system
            com.starmao.scannable.client.scanning.ScanResultProviders.initialize();

            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post evt) -> {
                ScanManager.tick();
            });

            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterLevel evt) -> {
                PoseStack poseStack = new PoseStack();
                poseStack.last().pose().set(evt.getModelViewMatrix());
                ScannerRenderer.INSTANCE.render(evt.getModelViewMatrix());
                ScanManager.renderLevel(poseStack, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            });
        });
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SCANNER.get(), ScannerContainerScreen::new);
        event.register(Containers.BLOCK_MODULE_CONTAINER.get(), ConfigurableBlockScannerModuleContainerScreen::new);
        event.register(Containers.ENTITY_MODULE_CONTAINER.get(), ConfigurableEntityScannerModuleContainerScreen::new);
        event.register(Containers.ITEM_MODULE_CONTAINER.get(), ConfigurableItemScannerModuleContainerScreen::new);
    }

    private static void onRegisterPipelines(RegisterRenderPipelinesEvent ev) {
        ev.registerPipeline(ScanResultRenderType.PIPELINE);
        ev.registerPipeline(ScanResultRenderType.LINES_PIPELINE);
        ev.registerPipeline(ScanResultRenderType.ICON_PIPELINE);
        ev.registerPipeline(ScanResultRenderType.SHIMMER_PIPELINE);
        ev.registerPipeline(ScanResultRenderType.SCAN_PROGRESS_PIPELINE);
        ev.registerPipeline(ScanResultRenderType.SCAN_EFFECT_PIPELINE);
    }

    private ScannerClientSetup() {
    }
}
