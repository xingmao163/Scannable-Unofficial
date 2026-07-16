package com.starmao.scannable.common.network.message;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.common.inventory.ScannerContainer;
import com.starmao.scannable.common.scanning.ItemScannerService;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Client-to-server scan request for the item scanner module.
 *
 * <p>Sent when the player scans with an item scanner module installed.
 * The server reads the scanner from the player's hand, finds the item
 * module's configuration, executes the scan, and sends results back
 * via {@link S2CItemScanResult}.
 */
public record C2SItemScanRequest() implements CustomPacketPayload {
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, "c2s_item_scan");
    public static final Type<C2SItemScanRequest> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SItemScanRequest> STREAM_CODEC =
            StreamCodec.ofMember(
                    (msg, buf) -> {},
                    buf -> new C2SItemScanRequest()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ---- Handler (Server Side) ---- //

    public static void handle(final C2SItemScanRequest msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            // Find which hand holds the scanner
            ItemStack scanner = ItemStack.EMPTY;
            for (final InteractionHand hand : InteractionHand.values()) {
                final ItemStack stack = player.getItemInHand(hand);
                if (com.starmao.scannable.common.item.ScannerItem.isScanner(stack)) {
                    scanner = stack;
                    break;
                }
            }
            if (scanner.isEmpty()) return;

            // Read the item scanner module configuration
            final ScannerContainer container = ScannerContainer.of(scanner);
            final com.starmao.scannable.common.inventory.ContainerSlice activeModules = container.getActiveModules();

            List<ResourceLocation> targetItemIds = List.of();
            for (int slot = 0; slot < activeModules.getContainerSize(); slot++) {
                final ItemStack module = activeModules.getItem(slot);
                if (module.isEmpty()) continue;
                if (module.getItem() instanceof ConfigurableItemScannerModuleItem moduleItem) {
                    targetItemIds = moduleItem.getIds(module);
                    break;
                }
            }

            if (targetItemIds.isEmpty()) return;

            // Calculate energy cost and check energy
            if (!player.isCreative()) {
                long totalCost = 0;
                for (int slot = 0; slot < activeModules.getContainerSize(); slot++) {
                    final ItemStack module = activeModules.getItem(slot);
                    if (module.isEmpty()) continue;
                    totalCost += ModuleHelper.getEnergyCost(module);
                }
                if (totalCost <= 0) totalCost = 75;

                final var energy = com.starmao.scannable.common.energy.ItemEnergyStorage.of(scanner);
                if (energy.isEmpty() || energy.get().extractEnergy(totalCost, true) < totalCost) {
                    player.displayClientMessage(
                            com.starmao.scannable.common.config.Strings.MESSAGE_NOT_ENOUGH_ENERGY, true);
                    return;
                }
                energy.get().extractEnergy(totalCost, false);
            }

            // Execute the scan on the server
            final Vec3 center = player.position();
            final int radius = 64;
            final List<ItemScanResultData> results = ItemScannerService.scan(
                    player.level(), center, radius, targetItemIds);

            if (ModConfig.DEBUG_LOG_ITEM_SCANNER.get()) {
                Scannable.LOGGER.info("[ItemScanner] Server scan complete: {} result(s)", results.size());
            }

            // Send results back to the client
            PacketDistributor.sendToPlayer(player, new S2CItemScanResult(center, results));
        });
    }
}
