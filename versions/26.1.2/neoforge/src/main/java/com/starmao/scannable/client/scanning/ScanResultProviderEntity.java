package com.starmao.scannable.client.scanning;

import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.Identifier;
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
            if (stack.getItem() instanceof EntityScannerModule module) {
                Predicate<Entity> filter = module.getFilter(stack);
                filters.add(filter);
                filterToModule.put(filter, module);
            }
        }
    }

    @Override
    public void computeScanResults() {
        if (player == null) return;
        Level level = player.level();
        if (level == null) return;

        final float radius = this.radius;
        final Vec3 center = this.center;
        final AABB area = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        final int step = 5;
        entitiesStep++;
        if (entitiesStep >= step) {
            entitiesStep = 0;
        } else {
            return;
        }

        if (currentEntityIndex == 0) {
            entities.clear();
            entities.addAll(level.getEntitiesOfClass(Entity.class, area, entity ->
                    entity != player && entity.isAlive()));
        }

        int processed = 0;
        while (currentEntityIndex < entities.size() && processed < 50) {
            Entity entity = entities.get(currentEntityIndex);
            currentEntityIndex++;
            processed++;

            for (Predicate<Entity> filter : filters) {
                if (filter.test(entity)) {
                    EntityScannerModule module = filterToModule.get(filter);
                    if (module != null) {
                        results.add(new ScanResultEntity(entity, (Identifier) ((Optional) module.getIcon(entity)).orElse(com.starmao.scannable.Scannable.id("textures/gui/overlay/info.png"))));
                    }
                    break;
                }
            }
        }

        if (currentEntityIndex >= entities.size()) {
            currentEntityIndex = 0;
        }
    }

    @Override
    public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) {
        results.forEach(callback);
    }

    @Override
    public void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                        Camera renderInfo, float partialTicks, List<ScanResult> results) {
        // Entity scanning results rendered via entity highlight system
    }

    @Override
    public void reset() {
        super.reset();
        entities.clear();
        results.clear();
        currentEntityIndex = 0;
        entitiesStep = 0;
    }

    private record ScanResultEntity(Entity entity, Identifier icon) implements ScanResult {
        @Override
        public Vec3 getPosition() {
            return entity.position();
        }

        public Identifier getIcon() {
            return icon;
        }

        public Entity getEntity() {
            return entity;
        }
        @Override
        @Nullable
        public AABB getRenderBounds() {
            return entity.getBoundingBox();
        }
    }
}
