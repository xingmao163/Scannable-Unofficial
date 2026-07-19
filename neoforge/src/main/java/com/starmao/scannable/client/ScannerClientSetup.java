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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side initialisation for all scanner rendering, audio, and GUI components.
 * <p>Registered during {@link net.neoforged.fml.event.lifecycle.FMLClientSetupEvent}.
 * Sets up screen bindings, level-rendering hooks, overlay GUI layers, and shader
 * initialisation. Also wires the per-tick {@link ScanManager#tick()} and
 * {@link ScannerRenderer#render} calls into the NeoForge event bus.
 */
public final class ScannerClientSetup {
