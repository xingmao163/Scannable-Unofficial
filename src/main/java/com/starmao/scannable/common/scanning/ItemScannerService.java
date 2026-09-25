package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ItemScannerService {
    public static List<ItemScanResultData> scan(Level level, Vec3 center, int radius, List<Identifier> targetItemIds) {
        // TODO: Reimplement item scanning for 26.1.2 using ResourceHandler API
        return List.of();
    }
    private ItemScannerService() {}
}