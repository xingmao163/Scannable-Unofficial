package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.BlockScannerModule;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.common.item.ModuleHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ScanResultProviderBlock extends AbstractScanResultProvider implements ScanResultProvider, BlockScannerModule {
    @Override public ScanResultProvider getResultProvider() { return this; }
    @Override public Predicate<BlockState> getFilter(ItemStack module) { return null; }
    @Override public int getEnergyCost(ItemStack stack) { return 0; }
    private List<Predicate<BlockState>> filters = List.of();

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);
        this.filters = buildFilters(modules);
    }

    @Override
    public void render(ScanResultRenderContext ctx, MultiBufferSource buf, PoseStack pose, Camera cam, float pt, List<ScanResult> results) {
        if (results.isEmpty()) return;

        if (ctx == ScanResultRenderContext.WORLD) {
            // Fill boxes with translucent color
            var fillConsumer = buf.getBuffer(com.starmao.scannable.client.renderer.ScanResultRenderType.RESULT_BOX_TYPE);
            for (ScanResult result : results) {
                if (!(result instanceof BlockScanResult br)) continue;
                double x = br.pos.getX();
                double y = br.pos.getY();
                double z = br.pos.getZ();
                var box = new AABB(x, y, z, x + 1, y + 1, z + 1);
                drawBox(fillConsumer, pose,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    0x44A8CCED);
            }

            // Shimmer overlay on top of fill boxes
            var shimmerConsumer = buf.getBuffer(com.starmao.scannable.client.renderer.ScanResultRenderType.SHIMMER_TYPE);
            for (ScanResult result : results) {
                if (!(result instanceof BlockScanResult br)) continue;
                double x = br.pos.getX();
                double y = br.pos.getY();
                double z = br.pos.getZ();
                var box = new AABB(x, y, z, x + 1, y + 1, z + 1);
                drawBox(shimmerConsumer, pose,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    0xCCAACCED);
            }
        }
    }

    @Override
    public void computeScanResults() {
        if (player == null || center == null || filters.isEmpty()) return;
        Level level = player.level();
        int r = this.radius;
        BlockPos minPos = BlockPos.containing(center).offset(-r, -r, -r);
        BlockPos maxPos = BlockPos.containing(center).offset(r, r, r);
        ChunkPos minChunk = ChunkPos.containing(minPos);
        ChunkPos maxChunk = ChunkPos.containing(maxPos);
        int minSec = Math.max(level.getSectionIndex(minPos.getY()), 0);
        int maxSec = Math.min(level.getSectionIndex(maxPos.getY()), level.getSectionsCount() - 1);
        for (int si = minSec; si <= maxSec; si++) {
            for (int cz = minChunk.z(); cz <= maxChunk.z(); cz++) {
                for (int cx = minChunk.x(); cx <= maxChunk.x(); cx++) {
                    int cy = level.getSectionYFromSectionIndex(si);
                    LevelChunkSection sec = level.getChunk(cx, cz).getSection(si);
                    if (sec == null || sec.hasOnlyAir()) continue;
                    for (int bx = 0; bx < 16; bx++)
                    for (int by = 0; by < 16; by++)
                    for (int bz = 0; bz < 16; bz++) {
                        BlockState state = sec.getBlockState(bx, by, bz);
                        if (!state.isAir() && filters.stream().anyMatch(f -> f.test(state)))
                            results.add(new BlockScanResult(new BlockPos(
                                SectionPos.sectionToBlockCoord(cx, bx),
                                SectionPos.sectionToBlockCoord(cy, by),
                                SectionPos.sectionToBlockCoord(cz, bz))));
                    }
                }
            }
        }
    }

    @Override
    public void collectScanResults(net.minecraft.world.level.BlockGetter level, Consumer<ScanResult> cb) {
        results.forEach(cb::accept);
    }

    private List<Predicate<BlockState>> buildFilters(Collection<ItemStack> modules) {
        List<Predicate<BlockState>> list = new ArrayList<>();
        for (ItemStack m : modules) {
            ModuleHelper.getModule(m).ifPresent(mod -> {
                if (mod instanceof BlockScannerModule bm) {
                    Predicate<BlockState> f = bm.getFilter(m);
                    if (f != null) list.add(f);
                }
            });
        }
        return list;
    }

    private final List<BlockScanResult> results = new ArrayList<>();
    private static class BlockScanResult implements ScanResult {
        final BlockPos pos;
        BlockScanResult(BlockPos p) { pos = p; }
        @Override public Vec3 getPosition() { return new Vec3(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5); }
        @Override @Nullable public AABB getRenderBounds() { return null; }
    }

    public void clearResults() { results.clear(); }
    @Override public boolean hasResultProvider() { return !filters.isEmpty(); }
}
