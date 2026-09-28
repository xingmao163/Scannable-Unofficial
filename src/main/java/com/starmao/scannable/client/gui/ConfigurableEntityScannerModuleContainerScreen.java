package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.EntityModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Screen for configuring an entity scanner module. */
public class ConfigurableEntityScannerModuleContainerScreen
        extends AbstractConfigurableScannerModuleContainerScreen<EntityModuleContainerMenu, EntityType<?>> {

    /** Side length of the box an entity model is fitted into, in GUI pixels. */
    private static final int SLOT_MODEL_BOX = 16;
    /** Target size, in pixels, of the entity's largest dimension inside the slot box. */
    private static final float SLOT_MODEL_SIZE = 13.0f;
    /** Vertical offset applied to the model, in model units, to lift it off the slot floor. */
    private static final float SLOT_MODEL_OFFSET_Y = 0.0625f;
    /** Yaw of the view in units of 20 degrees; 0.6 gives the 12 degree 3/4 view. */
    private static final float SLOT_MODEL_X_ANGLE = 0.6f;
    /** Downward pitch of the view in units of 20 degrees. */
    private static final float SLOT_MODEL_Y_ANGLE = 0.25f;

    private static final Map<EntityType<?>, Optional<LivingEntity>> RENDER_ENTITIES = new HashMap<>();

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

    /**
     * Draws the entity's 3D model into its configuration slot.
     *
     * <p>Called by {@link AbstractConfigurableScannerModuleContainerScreen#extractLabels} with
     * slot-relative coordinates, before the hover highlights are drawn. Rendering the model from
     * here — rather than from an {@code extractContents} override — keeps the highlight on top of
     * the model; drawing it after {@code super.extractContents()} would cover the highlight.
     *
     * <p><b>Why the {@code leftPos}/{@code topPos} offset is needed here but must NOT be used in
     * the item / block screens:</b> the two paths differ in how the extractor's
     * {@code Matrix3x2fStack} pose is handled.
     * {@code GuiGraphicsExtractor#entity} builds a {@code GuiEntityRenderState} that carries
     * <em>no</em> pose, so this needs absolute screen coordinates — omit the offset and the model
     * lands in the screen's top-left corner.
     * {@code fakeItem} is the opposite: it stores {@code new Matrix3x2f(this.pose)} in its
     * {@code GuiItemRenderState}, so item coordinates are already panel-relative and adding the
     * offset would double it.
     */
    @Override
    protected void renderConfiguredItem(GuiGraphicsExtractor graphics, EntityType<?> entityType, int x, int y) {
        final LivingEntity entity = getRenderEntity(entityType);
        if (entity == null) return;

        final int x0 = leftPos + x;
        final int y0 = topPos + y;

        // `size` is a direct pixel multiplier, so normalise by the entity's largest dimension to
        // fit any mob into the slot, and render at a gentle fixed 3/4 angle.
        final EntityDimensions dimensions = entityType.getDimensions();
        final int size = Math.max(1, (int) (SLOT_MODEL_SIZE / Math.max(dimensions.width(), dimensions.height())));
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics, x0, y0, x0 + SLOT_MODEL_BOX, y0 + SLOT_MODEL_BOX,
                size, SLOT_MODEL_OFFSET_Y, SLOT_MODEL_X_ANGLE, SLOT_MODEL_Y_ANGLE, entity);
    }

    /**
     * Caches a stand-in entity instance per type, created with the current level so that a world
     * switch never leaves a stale one around. Only living entities can be previewed; anything else
     * has no model to draw and is skipped.
     */
    private LivingEntity getRenderEntity(EntityType<?> entityType) {
        return RENDER_ENTITIES.computeIfAbsent(entityType, type -> {
            final Entity entity = type.create(menu.getPlayer().level(), EntitySpawnReason.LOAD);
            return entity instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
        }).orElse(null);
    }

    @Override
    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {
        if (value.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> entityType = egg.getType(value);
            BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).ifPresent(key ->
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.identifier())));
        }
    }
}
