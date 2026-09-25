package com.starmao.scannable;

import com.starmao.scannable.client.config.ClientConfig;
import com.starmao.scannable.common.capability.ScannerModuleCapability;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.container.Containers;
import com.starmao.scannable.common.inventory.ScannerContainer;
import com.starmao.scannable.common.inventory.ScannerItemHandler;
import com.starmao.scannable.common.item.Items;
import com.starmao.scannable.common.item.ModDataComponents;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.scanning.FluidBlockScannerModule;
import com.starmao.scannable.common.tags.ItemTags;
import com.starmao.scannable.registry.ModCreativeTabs;
import com.starmao.scannable.common.container.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unofficial NeoForge port of Scannable — a handheld scanner mod with swappable modules.
 *
 * <p>Original Scannable mod by Florian "Sangar" Nucke (MightyPirates), MIT licensed.
 * See LICENSE in the project root for full terms. Textures/localizations are CC0.
 */
@Mod(Scannable.MOD_ID)
public final class Scannable {
    public static final String MOD_ID = "scannable_unofficial";
    public static final Logger LOGGER = LoggerFactory.getLogger(Scannable.class);

    public Scannable(IEventBus modEventBus, ModContainer modContainer) {
        // --- Registration ---
        ModDataComponents.register(modEventBus);
        Items.register(modEventBus);
        ModMenus.register(modEventBus);
        Containers.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        ItemTags.initialize();

        // --- Capabilities ---
        modEventBus.addListener(Scannable::registerCapabilities);
        Network.register(modEventBus);

        modContainer.registerConfig(Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(Type.CLIENT, ClientConfig.SPEC);

        modEventBus.addListener(Scannable::onModConfigEvent);

        // --- Client-only setup ---
        if (FMLEnvironment.dist.isClient()) {
            com.starmao.scannable.client.ScannerClientSetup.initialize(modEventBus);

            // Make the Config button clickable in the Mods screen.
            modContainer.registerExtensionPoint(
                    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (net.neoforged.neoforge.client.gui.IConfigScreenFactory) (mc, screen) ->
                            new net.neoforged.neoforge.client.gui.ConfigurationScreen(modContainer, screen)
            );
        }

    }

    private static void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            FluidBlockScannerModule.clearCache();
            com.starmao.scannable.common.scanning.filter.IgnoredBlocks.clearCache();
        } else if (event.getConfig().getSpec() == ClientConfig.SPEC) {
            com.starmao.scannable.client.config.ClientConfig.clearCache();
            com.starmao.scannable.client.scanning.ProviderCacheManager.clearCache();
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Energy storage — enables FE charge/discharge on the scanner item
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, ctx) -> com.starmao.scannable.common.energy.neoforge.ScannerEnergyStorage.of(stack),
                Items.SCANNER.get());

        // Item handler — exposes the scanner's internal module inventory
        // to hoppers, other mods, etc.
        event.registerItem(Capabilities.ItemHandler.ITEM,
                (stack, ctx) -> new ScannerItemHandler(ScannerContainer.of(stack)),
                Items.SCANNER.get());

        // ScannerModule capability — uniform access to scanning behaviour
        // across all module item types without instanceof checks.
        ScannerModuleCapability.register(event,
                Items.RANGE_MODULE.get(),
                Items.ENTITY_MODULE.get(),
                Items.FRIENDLY_ENTITY_MODULE.get(),
                Items.HOSTILE_ENTITY_MODULE.get(),
                Items.BLOCK_MODULE.get(),
                Items.FLUID_MODULE.get(),
                Items.ITEM_MODULE.get());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
