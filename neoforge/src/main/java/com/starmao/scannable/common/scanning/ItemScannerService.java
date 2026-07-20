package com.starmao.scannable.common.scanning;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless server-side scanner for the item scanner module.
 *
 * <p>Scans all loaded chunks within the given radius for block entities
 * that expose an {@link IItemHandler} capability, then checks their
 * inventory contents against the configured target items.
 *
 * <p>This is designed to run on the server thread where container
 * inventory data is fully available (unlike the client side where
 * unopened containers have empty inventories).
 */
public final class ItemScannerService {

    /**
     * Execute a scan for the given items within a radius around the centre position.
     *
     * @param level    The level to scan in (server-side)
     * @param center   The centre position of the scan
     * @param radius   The scan radius in blocks
     * @param targetItemIds Registry names of items to search for
     * @return List of scan results (non-empty containers with matching items)
     */
    public static List<ItemScanResultData> scan(
            final Level level,
            final Vec3 center,
            final int radius,
            final List<ResourceLocation> targetItemIds) {

        final List<ItemScanResultData> results = new ArrayList<>();
        if (targetItemIds == null || targetItemIds.isEmpty()) return results;

        // Resolve target items from registry names
        final List<Item> targetItems = new ArrayList<>();
        for (final ResourceLocation id : targetItemIds) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(targetItems::add);
        }
        if (targetItems.isEmpty()) return results;

        final double sqRadius = (double) radius * radius;

        // Only scan loaded chunks within range
        final int minCX = (int) Math.floor((center.x - radius) / 16);
        final int maxCX = (int) Math.ceil((center.x + radius) / 16);
        final int minCZ = (int) Math.floor((center.z - radius) / 16);
        final int maxCZ = (int) Math.ceil((center.z + radius) / 16);

        int chunksChecked = 0;
        int containersFound = 0;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                chunksChecked++;

                // Iterate block entities in the chunk (much more efficient
                // than scanning every block position)
                final var chunk = level.getChunk(cx, cz);
                for (final BlockEntity be : chunk.getBlockEntities().values()) {
                    final BlockPos pos = be.getBlockPos();

                    // Quick distance check
                    final double dx = pos.getX() + 0.5 - center.x;
                    final double dy = pos.getY() + 0.5 - center.y;
                    final double dz = pos.getZ() + 0.5 - center.z;
                    if (dx * dx + dy * dy + dz * dz > sqRadius) continue;

                    // Query the default (non-sided) handler. The null context returns a handler
                    // covering all accessible slots for virtually all blocks (chests, furnaces,
                    // barrels, hoppers, etc.). Per-direction queries are not performed because
                    // they return separate wrapper instances with independent slot numbering.
                    final IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    if (handler == null) continue;
                    containersFound++;

                    final Map<Item, Integer> posTotal = new HashMap<>();
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        final ItemStack stack = handler.getStackInSlot(slot);
                        if (stack.isEmpty()) continue;
                        final Item item = stack.getItem();
                        if (!targetItems.contains(item)) continue;
                        posTotal.merge(item, stack.getCount(), Integer::sum);
                    }
                    for (final var entry : posTotal.entrySet()) {
                        results.add(new ItemScanResultData(
                                pos,
                                BuiltInRegistries.ITEM.getKey(entry.getKey()),
                                entry.getValue()));
                    }
                }
            }
        }

        if (ModConfig.DEBUG_LOG_ITEM_SCANNER.get()) {
            Scannable.LOGGER.info("[ItemScannerService] Chunks: {}, Containers: {}, Matches: {}",
                    chunksChecked, containersFound, results.size());
        }

        return results;
    }


    private ItemScannerService() {}
}
