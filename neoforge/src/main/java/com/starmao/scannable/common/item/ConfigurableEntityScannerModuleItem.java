package com.starmao.scannable.common.item;

import com.starmao.scannable.common.container.EntityModuleContainerMenu;
import com.starmao.scannable.common.scanning.ConfigurableEntityScannerModule;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Item for the configurable entity scanner module.
 * <p>Right-clicking an entity adds its type to the module's target list.
 * Opens a configuration GUI when shift-right-clicked in the air.
 *
 * @see ConfigurableEntityScannerModule
 * @see EntityModuleContainerMenu
 */
public final class ConfigurableEntityScannerModuleItem extends ConfigurableModuleItem<EntityType<?>> {
    public ConfigurableEntityScannerModuleItem() {
        super(ConfigurableEntityScannerModule.INSTANCE,
                (id, inv, hand) -> new EntityModuleContainerMenu(id, inv, hand));
    }

    @Override
    protected DataComponentType<List<ResourceLocation>> getComponent() {
        return ModDataComponents.ENTITY_TYPES.get();
    }

    @Override
    protected Registry<EntityType<?>> getRegistry() {
        return BuiltInRegistries.ENTITY_TYPE;
    }

    @Override
    protected Component getDisplayName(final EntityType<?> entityType) {
        return entityType.getDescription();
    }

    @Override
    protected Component getListCaption() {
        return Component.translatable("tooltip.scannable_unofficial.scanner.entity_module.list").withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    @Override
    public InteractionResult interactLivingEntity(final ItemStack stack, final Player player, final LivingEntity target, final InteractionHand hand) {
        if (addValue(player.getItemInHand(hand), target.getType())) {
            player.swing(hand);
            player.getInventory().setChanged();
        } else {
            if (!player.level().isClientSide() && !isLocked(stack)) {
                player.displayClientMessage(
                        Component.translatable("message.scannable_unofficial.scanner.no_free_slots").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
        return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
