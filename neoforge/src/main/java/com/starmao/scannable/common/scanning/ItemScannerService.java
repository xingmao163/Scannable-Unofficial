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

        final BlockPos centrePos = BlockPos.containing(center);
        final int radiusSq = radius * radius;

        // Iterate in a cubic bounding box around the player
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    final BlockPos pos = centrePos.offset(dx, dy, dz);
                    if (centrePos.distSqr(pos) > radiusSq) continue;

                    final BlockEntity be = level.getBlockEntity(pos);
                    if (be == null) continue;

                    final IItemHandler handler = level.getCapability(
                            Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
                    if (handler == null) continue;

                    scanHandler(handler, targetItems, pos, results);
                }
            }
        }

        if (ModConfig.DEBUG_LOG_ITEM_SCANNER.get()) {
            Scannable.LOGGER.info("[ItemScannerService] Scanned radius {} around {} ({} chunk(s)), found {} result(s)",
                    radius, centrePos, results.size());
        }

        return results;
    }

    private static void scanHandler(
            final IItemHandler handler,
            final List<Item> targetItems,
            final BlockPos pos,
            final List<ItemScanResultData> results) {

        final Map<Item, Integer> matches = new HashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (targetItems.contains(stack.getItem())) {
                matches.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        if (!matches.isEmpty()) {
            for (final Map.Entry<Item, Integer> entry : matches.entrySet()) {
                results.add(new ItemScanResultData(
                        pos,
                        BuiltInRegistries.ITEM.getKey(entry.getKey()),
                        entry.getValue()));
            }
        }
    }

    private ItemScannerService() {}
}
