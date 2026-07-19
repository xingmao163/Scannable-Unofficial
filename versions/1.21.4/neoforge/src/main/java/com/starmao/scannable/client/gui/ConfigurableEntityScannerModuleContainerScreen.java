package com.starmao.scannable.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.common.container.EntityModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Screen for configuring an entity scanner module. */
public class ConfigurableEntityScannerModuleContainerScreen
        extends AbstractConfigurableScannerModuleContainerScreen<EntityModuleContainerMenu, EntityType<?>> {

    private static final Map<EntityType<?>, Entity> RENDER_ENTITIES = new HashMap<>();

    public ConfigurableEntityScannerModuleContainerScreen(EntityModuleContainerMenu container,
                                                           Inventory inventory, Component title) {
        super(container, inventory, title,
                Component.translatable("gui.scannable_unofficial.scanner.entity_module.list"));
    }

    protected ConfigurableEntityScannerModuleContainerScreen(EntityModuleContainerMenu container,
                                                              Inventory inventory, Component title, Component listCaption) {
        super(container, inventory, title, listCaption);
    }

    @Override
    protected List<EntityType<?>> getConfiguredItems(ItemStack stack) {
        if (stack.getItem() instanceof ConfigurableEntityScannerModuleItem item) {
            return item.getValues(stack);
        }
        return List.of();
    }

    @Override
    protected Component getItemName(EntityType<?> entityType) {
        return entityType.getDescription();
    }

    @Override
    protected void renderConfiguredItem(GuiGraphics graphics, EntityType<?> entityType, int x, int y) {
        renderEntity(graphics, x + 8, y + 13, entityType);
    }

    @Override
    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {
        if (value.getItem() instanceof SpawnEggItem egg) {
            var provider = menu.getPlayer().level().registryAccess();
            EntityType<?> entityType = egg.getType(provider, value);
            BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).ifPresent(key ->
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.location())));
        }
    }

    private void renderEntity(GuiGraphics graphics, int x, int y, EntityType<?> entityType) {
        Entity entity = getRenderEntity(entityType);
        if (entity == null) return;

        EntityDimensions bounds = entityType.getDimensions();
        float size = Math.max(bounds.width(), bounds.height());
        float scale = 11.0f / size;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 100);
        poseStack.scale(scale, scale, scale);

        var quaternion = new Quaternionf().rotationZ((float) Math.toRadians(180));
        quaternion.mul(new Quaternionf().rotationX((float) Math.toRadians(20)));
        quaternion.mul(new Quaternionf().rotationY((float) Math.toRadians(30)));
        poseStack.mulPose(quaternion);

        var renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion.conjugate();
        renderManager.overrideCameraOrientation(quaternion);
        renderManager.setRenderShadow(false);
        renderManager.render(entity, 0, 0, 0, 1, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 0xf000f0);
        renderManager.setRenderShadow(true);
        poseStack.popPose();
    }

    @Nullable
    private Entity getRenderEntity(EntityType<?> entityType) {
        return RENDER_ENTITIES.computeIfAbsent(entityType, t -> t.create(menu.getPlayer().level(), EntitySpawnReason.NATURAL));
    }
}
