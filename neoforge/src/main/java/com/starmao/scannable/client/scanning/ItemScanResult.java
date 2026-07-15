package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.ScanResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Client-side scan result for the item scanner module.
 *
 * <p>Holds the position of a matching container, the matched item,
 * and the total count. Used by {@link ScanResultProviderItem} for
 * rendering container highlights and item labels.
 *
 * <p>Instances may be created from server-side scan results
 * ({@link com.starmao.scannable.network.ItemScanResultData})
 * or from client-side chunk scanning.
 */
public record ItemScanResult(BlockPos pos, ItemStack item, int totalCount, int blockColor) implements ScanResult {
    @Override
    public Vec3 getPosition() {
        return Vec3.atCenterOf(pos);
    }

    @Nullable
    @Override
    public AABB getRenderBounds() {
        return new AABB(pos);
    }
}
