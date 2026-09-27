package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.EntityScannerModule;
import com.starmao.scannable.api.ModTextures;
import com.starmao.scannable.api.ScanResult;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultRenderContext;
import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.api.template.AbstractScanResultProvider;
import com.starmao.scannable.common.item.ModuleHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public final class ScanResultProviderEntity extends AbstractScanResultProvider implements ScanResultProvider {
    private final List<Predicate<Entity>> filters = new ArrayList<>();
    private final Map<Predicate<Entity>, EntityScannerModule> filterToModule = new HashMap<>();
    private final ArrayList<Entity> entities = new ArrayList<>();
    private int currentEntityIndex, entitiesStep;
    private final List<EntityScanResult> results = new ArrayList<>();

    // ----------------------------------------------------------------- //

    @Override
    public void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);
        filters.clear();
        filterToModule.clear();
        for (ItemStack stack : modules) {
            ModuleHelper.getModule(stack).ifPresent(module -> {
                if (module instanceof EntityScannerModule em) {
                    Predicate<Entity> filter = em.getFilter(stack);
                    filters.add(filter);
                    filterToModule.put(filter, em);
                }
            });
        }

        entities.clear();
        // Query only the entities inside the scan sphere instead of walking every loaded
        // entity in the level. LevelEntityGetter#get(AABB, Consumer) is spatially indexed,
        // so the cost scales with what is actually in range.
        final AABB scanBounds = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        player.level().getEntities().get(scanBounds, entities::add);
        currentEntityIndex = 0;
        entitiesStep = Mth.ceil(entities.size() / (float) scanTicks);
    }

    @Override
    public void computeScanResults() {
        for (int end = Math.min(currentEntityIndex + entitiesStep, entities.size());
             currentEntityIndex < end; currentEntityIndex++) {
            Entity entity = entities.get(currentEntityIndex);
            if (!entity.isAlive()) {
                continue;
            }
            if (center.distanceToSqr(entity.position()) < (double) radius * radius) {
                Identifier icon = ModTextures.ICON_INFO;
                boolean hasMatch = false;
                for (Predicate<Entity> filter : filters) {
                    if (filter.test(entity)) {
                        hasMatch = true;
                        EntityScannerModule mod = filterToModule.get(filter);
                        if (mod != null) {
                            Optional<Identifier> filterIcon = mod.getIcon(entity);
                            if (filterIcon.isPresent()) {
                                icon = filterIcon.get();
                            }
                        }
                        break;
                    }
                }
                if (hasMatch) {
                    results.add(new EntityScanResult(entity, icon));
                }
            }
        }
    }

    @Override
    public void collectScanResults(BlockGetter level, Consumer<ScanResult> callback) {
        results.forEach(callback::accept);
    }

    @Override
    public void render(ScanResultRenderContext ctx, MultiBufferSource buf, PoseStack pose, Camera cam, float pt, List<ScanResult> results) {
        if (ctx != ScanResultRenderContext.GUI || results.isEmpty()) {
            return;
        }

        org.joml.Vector3fc forward = cam.forwardVector();
        Vec3 lookVec = new Vec3(forward.x(), forward.y(), forward.z());
        Vec3 viewerEyes = cam.position();
        float yaw = cam.yRot();
        float pitch = cam.xRot();
        boolean showDistance = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.isShiftKeyDown();

        // Sort ascending by how closely each entity is aimed at, so the best-aimed ones sit at the
        // END of the list — the loop below walks backwards from there.
        results.sort(Comparator.comparing(result -> {
            EntityScanResult er = (EntityScanResult) result;
            Vec3 eyePos = er.entity.getEyePosition(pt);
            return lookVec.dot(eyePos.subtract(viewerEyes).normalize());
        }));

        // Walk from the best-aimed entity backwards, drawing at most MAX_ICONS of them, mirroring
        // renderIconLabels: the cap keeps icons from piling up and favours what the player is most
        // likely looking at.
        int shown = 0;
        for (int i = results.size() - 1; i >= 0 && shown < MAX_ICONS; i--) {
            EntityScanResult er = (EntityScanResult) results.get(i);
            // No look-direction gate here: renderIconLabel draws the icon for every result and
            // gates only the text label on the crosshair, matching the 1.21.1 overlay. Filtering
            // on the look vector at this level would hide the icons too, making a scan appear to
            // have found nothing until the player aims at each entity.
            Vec3 eyePos = er.entity.getEyePosition(pt);
            Component label = er.entity.getName();
            float distance = showDistance ? (float) eyePos.subtract(viewerEyes).length() : 0f;
            renderIconLabel(buf, pose, yaw, pitch, lookVec, viewerEyes, distance, eyePos,
                    er.icon, label);
            shown++;
        }
    }

    @Override
    public void reset() {
        super.reset();
        filters.clear();
        filterToModule.clear();
        currentEntityIndex = entitiesStep = 0;
        entities.clear();
        results.clear();
    }

    // ----------------------------------------------------------------- //

    private static final class EntityScanResult implements ScanResult {
        private final Entity entity;
        private final Identifier icon;

        EntityScanResult(Entity entity, Identifier icon) {
            this.entity = entity;
            this.icon = icon;
        }

        @Override public Vec3 getPosition() { return entity.position(); }
        @Override @Nullable public AABB getRenderBounds() { return entity.getBoundingBox(); }
    }
}
