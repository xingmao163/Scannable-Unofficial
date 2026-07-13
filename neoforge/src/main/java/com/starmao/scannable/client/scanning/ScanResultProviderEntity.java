package com.starmao.scannable.client.scanning;

import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ScanResultProviderEntity extends AbstractScanResultProvider {
    private final List<Predicate<Entity>> filters = new ArrayList<>();
    private final Map<Predicate<Entity>, EntityScannerModule> filterToModule = new HashMap<>();
    private final ArrayList<Entity> entities = new ArrayList<>();
    private int currentEntityIndex, entitiesStep;
    private final List<ScanResultEntity> results = new ArrayList<>();

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);

        filters.clear();
        filterToModule.clear();
        for (ItemStack stack : modules) {
            ModuleHelper.getModule(stack).ifPresent(module -> {
                if (module instanceof EntityScannerModule entityModule) {
                    Predicate<Entity> filter = entityModule.getFilter(stack);
                    filters.add(filter);
                    filterToModule.put(filter, entityModule);
                }
            });
        }

        entities.clear();
        // Use getEntitiesOfClass to get all entities in the scan radius (avoids LevelChunk API issues)
        Level level = player.level();
        AABB scanArea = new AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        );
        List<Entity> allEntities = level.getEntitiesOfClass(Entity.class, scanArea, entity -> true);
        entities.addAll(allEntities);

        currentEntityIndex = 0;
        entitiesStep = Math.max(1, Mth.ceil(entities.size() / (float) scanTicks));
    }

    @Override
    public void computeScanResults() {
        for (int end = Math.min(currentEntityIndex + entitiesStep, entities.size()); currentEntityIndex < end; currentEntityIndex++) {
            Entity entity = entities.get(currentEntityIndex);
            if (!entity.isAlive()) continue;

            Vec3 position = entity.position();
            if (center.distanceToSqr(position) < radius * radius) {
                ResourceLocation icon = ModTextures.ICON_INFO;
                boolean hasMatch = false;
                for (Predicate<Entity> filter : filters) {
                    if (filter.test(entity)) {
                        hasMatch = true;
                        Optional<ResourceLocation> filterIcon = filterToModule.get(filter).getIcon(entity);
                        if (filterIcon.isPresent()) {
                            icon = filterIcon.get();
                            break;
                        }
                    }
                }
                if (hasMatch) {
                    results.add(new ScanResultEntity(entity, icon));
                }
            }
        }
    }

    @Override
    public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) {
        results.forEach(callback);
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                        Camera renderInfo, float partialTicks, List<ScanResult> results) {
        if (context != ScanResultRenderContext.GUI) return;

        float yaw = renderInfo.getYRot();
        float pitch = renderInfo.getXRot();
        Vec3 lookVec = new Vec3(renderInfo.getLookVector());
        Vec3 viewerEyes = renderInfo.getPosition();
        boolean showDistance = renderInfo.getEntity().isShiftKeyDown();

        results.sort(Comparator.comparing(result ->
                lookVec.dot(((ScanResultEntity) result).entity.getEyePosition(partialTicks).subtract(viewerEyes).normalize())));

        renderIconLabels(bufferSource, poseStack, yaw, pitch, lookVec, viewerEyes, showDistance, results,
                result -> ((ScanResultEntity) result).entity.getEyePosition(partialTicks),
                result -> ((ScanResultEntity) result).getIcon(),
                result -> ((ScanResultEntity) result).entity.getName(),
                result -> true,
                Integer.MAX_VALUE, -1f);
    }

    @Override
    public void reset() {
        super.reset();
        filters.clear();
        filterToModule.clear();
        currentEntityIndex = 0;
        entitiesStep = 0;
        entities.clear();
        results.clear();
    }

    private record ScanResultEntity(Entity entity, ResourceLocation icon) implements ScanResult {
        public ResourceLocation getIcon() { return icon; }

        @Override
        public Vec3 getPosition() { return entity.position(); }

        @Override
        public AABB getRenderBounds() { return entity.getBoundingBoxForCulling(); }
    }
}