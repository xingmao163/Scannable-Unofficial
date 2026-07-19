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

    // Per-screen cache of sample entities for the slot previews (created with the current level so a
    // world switch never leaves a stale one around; living entities only — others render as blank).
    private final Map<EntityType<?>, Optional<LivingEntity>> renderEntities = new HashMap<>();

    public ConfigurableEntityScannerModuleContainerScreen(final EntityModuleContainerMenu container,
                                                           final Inventory inventory, final Component title) {
        this(container, inventory, title,
                Component.translatable("gui.scannable_unofficial.scanner.entity_module.list"));
    }

    // Lets the spawner module subclass reuse this whole screen (incl. the mob previews) with its own
    // list caption.
    protected ConfigurableEntityScannerModuleContainerScreen(final EntityModuleContainerMenu container,
                                                              final Inventory inventory, final Component title,
                                                              final Component listCaption) {
        super(container, inventory, title, listCaption);
    }

    @Override
    protected List<EntityType<?>> getConfiguredItems(final ItemStack stack) {
        if (stack.getItem() instanceof ConfigurableEntityScannerModuleItem item) {
            return item.getValues(stack);
        }
        return List.of();
    }

    @Override
    protected Component getItemName(final EntityType<?> entityType) {
        return entityType.getDescription();
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        // The entity previews are 3D picture-in-picture renders that take absolute screen coordinates
        // (like the vanilla inventory player model), so they go here in the un-translated content
        // phase rather than the 2D renderConfiguredItem (label) path.
        final ItemStack stack = menu.getPlayer().getItemInHand(menu.getHand());
        final List<EntityType<?>> items = getConfiguredItems(stack);
        for (int slot = 0; slot < items.size() && slot < 5; slot++) {
            final LivingEntity entity = getRenderEntity(items.get(slot));
            if (entity == null) continue;

            final int x = leftPos + SLOTS_ORIGIN_X + slot * SLOT_SIZE;
            final int y = topPos + SLOTS_ORIGIN_Y;

            // The size is a direct pixel multiplier, so normalise by the entity's largest dimension to
            // fit any mob into the 16x16 slot; render at a gentle fixed 3/4 angle.
            final EntityDimensions dimensions = items.get(slot).getDimensions();
            final int size = Math.max(1, (int) (13.0f / Math.max(dimensions.width(), dimensions.height())));
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, x, y, x + 16, y + 16, size, 0.0625f, 0.6f, 0.25f, entity);
        }
    }

    @Override
    protected void renderConfiguredItem(final GuiGraphicsExtractor graphics, final EntityType<?> entityType, final int x, final int y) {
        // Entity previews are drawn in extractContents (see above); nothing to do in the label phase.
    }

    @Override
    protected void configureItemAt(final ItemStack stack, final int slot, final ItemStack value) {
        if (value.getItem() instanceof SpawnEggItem egg) {
            final EntityType<?> entityType = egg.getType(value);
            BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).ifPresent(key ->
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.identifier())));
        }
    }

    private LivingEntity getRenderEntity(final EntityType<?> entityType) {
        return renderEntities.computeIfAbsent(entityType, type -> {
            final Entity entity = type.create(menu.getPlayer().level(), EntitySpawnReason.LOAD);
            return entity instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
        }).orElse(null);
    }
}
