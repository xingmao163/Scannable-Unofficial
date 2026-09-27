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

    @Override
    protected void renderConfiguredItem(GuiGraphicsExtractor graphics, EntityType<?> entityType, int x, int y) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        final ItemStack stack = getHeldItem();
        final List<EntityType<?>> items = getConfiguredItems(stack);
        for (int slot = 0; slot < Math.min(items.size(), SLOT_COUNT); slot++) {
            final LivingEntity entity = getRenderEntity(items.get(slot));
            if (entity == null) continue;

            final int x = leftPos + SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            final int y = topPos + SLOTS_ORIGIN_Y;

            // `size` is a direct pixel multiplier, so normalise by the entity's largest dimension to
            // fit any mob into the slot, and render at a gentle fixed 3/4 angle.
            final EntityDimensions dimensions = items.get(slot).getDimensions();
            final int size = Math.max(1, (int) (SLOT_MODEL_SIZE / Math.max(dimensions.width(), dimensions.height())));
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                    graphics, x, y, x + SLOT_MODEL_BOX, y + SLOT_MODEL_BOX,
                    size, SLOT_MODEL_OFFSET_Y, SLOT_MODEL_X_ANGLE, SLOT_MODEL_Y_ANGLE, entity);
        }
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
