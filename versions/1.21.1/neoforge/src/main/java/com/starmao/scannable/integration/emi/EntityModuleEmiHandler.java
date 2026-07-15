package com.starmao.scannable.integration.emi;

import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

/**
 * Drag-drop handler for the configurable entity scanner module screen.
 * Allows dragging entity spawn eggs from EMI's sidebar directly onto module configuration slots.
 */
public class EntityModuleEmiHandler implements EmiDragDropHandler<ConfigurableEntityScannerModuleContainerScreen> {

    @Override
    public boolean dropStack(
            final ConfigurableEntityScannerModuleContainerScreen screen,
            final EmiIngredient ingredient,
            final int x,
            final int y) {

        // Extract ItemStack from EmiIngredient
        final var itemStackOpt = ingredient.getItemStack();
        if (itemStackOpt.isEmpty()) {
            return false;
        }

        final var itemStack = itemStackOpt.get();
        final EntityType<?> entityType = EntityType.getEntityTypeFromItem(itemStack.getItem());
        if (entityType == null) {
            return false;
        }

        // Calculate which slot was clicked
        final int slotIndex = calculateSlotIndex(screen, x, y);
        if (slotIndex < 0 || slotIndex >= 5) {
            return false;
        }

        // Send network packet to configure the module
        BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).ifPresent(key ->
            Network.sendToServer(new SetConfiguredModuleItemAtMessage(
                screen.getMenu().containerId,
                slotIndex,
                key.location())));

        return true;
    }

    /**
     * Calculates which slot (0-4) the given screen coordinates fall into.
     * Returns -1 if the coordinates don't fall within any slot.
     */
    private int calculateSlotIndex(
            final ConfigurableEntityScannerModuleContainerScreen screen,
            final int x,
            final int y) {

        final int guiLeft = screen.getGuiLeft();
        final int guiTop = screen.getGuiTop();
        final int originX = guiLeft + ConfigurableEntityScannerModuleContainerScreen.SLOTS_ORIGIN_X;
        final int originY = guiTop + ConfigurableEntityScannerModuleContainerScreen.SLOTS_ORIGIN_Y;
        final int slotSize = ConfigurableEntityScannerModuleContainerScreen.SLOT_SIZE;

        final int relX = x - originX;
        final int relY = y - originY;

        // Check if Y is within a single slot row
        if (relY < 0 || relY >= slotSize) {
            return -1;
        }

        // Check if X falls within the 5-slot row
        if (relX < 0 || relX >= slotSize * 5) {
            return -1;
        }

        return relX / slotSize;
    }
}
