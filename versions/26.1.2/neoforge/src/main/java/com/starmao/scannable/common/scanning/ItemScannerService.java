package com.starmao.scannable.common.scanning;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless server-side scanner for the item scanner module.
 *
 * <p>Scans all loaded chunks within the given radius for block entities
 * that expose a {@link ResourceHandler} capability, then checks their
 * inventory contents against the configured target items.
 *
 * <p>In NeoForge 26.1.2, the old {@code IItemHandler} capability was
 * replaced by the transfer API: {@code Capabilities.Item.BLOCK} returns
 * a {@code ResourceHandler<ItemResource>}. Slots are accessed via
 * {@link ResourceHandler#size()}, {@link ResourceHandler#getResource(int)},
 * and {@link ResourceHandler#getAmountAsInt(int)}.
 *
 * <p>This is designed to run on the server thread where container
 * inventory data is fully available (unlike the client side where
 * unopened containers have empty inventories).
 */
public final class ItemScannerService {

    /**
     * Execute a scan for the given items within a radius around the center position.
     *
     * @param level    The level to scan in (server-side)
     * @param center   The center position of the scan
     * @param radius   The scan radius in blocks
     * @param targetItemIds Registry names of items to search for
     * @return List of scan results (non-empty containers with matching items)
     */
    public static List<ItemScanResultData> scan(
            final Level level,
            final Vec3 center,
            final int radius,
            final List<Identifier> targetItemIds) {

        final List<ItemScanResultData> results = new ArrayList<>();
        if (targetItemIds == null || targetItemIds.isEmpty()) return results;

        // Resolve target items from registry names
        final List<Item> targetItems = new ArrayList<>();
        for (final Identifier id : targetItemIds) {
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

                    // Collect unique handlers from all directions to catch
                    // sided inventories (furnace fuel slot, etc.).
                    final java.util.IdentityHashMap<ResourceHandler<ItemResource>, Boolean> seen = new java.util.IdentityHashMap<>();
                    for (final Direction dir : Direction.values()) {
                        final ResourceHandler<ItemResource> h = level.getCapability(Capabilities.Item.BLOCK, pos, dir);
                        if (h != null) seen.put(h, Boolean.TRUE);
                    }
                    {
                        final ResourceHandler<ItemResource> h = level.getCapability(Capabilities.Item.BLOCK, pos, null);
                        if (h != null) seen.put(h, Boolean.TRUE);
                    }
                    if (seen.isEmpty()) continue;
                    containersFound++;

                    // Deduplicate by (item, count) pairs across all handlers at
                    // this position to avoid counting the same physical slot
                    // through different handler instances.
                    final java.util.Map<Item, Integer> posTotal = new java.util.HashMap<>();
                    final java.util.Set<String> seenSlotKeys = new java.util.HashSet<>();
                    for (final ResourceHandler<ItemResource> handler : seen.keySet()) {
                        for (int slot = 0; slot < handler.size(); slot++) {
                            final ItemResource resource = handler.getResource(slot);
                            if (resource.isEmpty()) continue;
                            final Item item = resource.getItem();
                            if (!targetItems.contains(item)) continue;
                            final int count = handler.getAmountAsInt(slot);
                            final String slotKey = BuiltInRegistries.ITEM.getKey(item) + ":" + count;
                            if (!seenSlotKeys.add(slotKey)) continue;
                            posTotal.merge(item, count, Integer::sum);
                        }
                    }
                    for (final var entry : posTotal.entrySet()) {
                        final Identifier matchedId = BuiltInRegistries.ITEM.getKey(entry.getKey());
                        results.add(new ItemScanResultData(pos, matchedId, entry.getValue()));
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
