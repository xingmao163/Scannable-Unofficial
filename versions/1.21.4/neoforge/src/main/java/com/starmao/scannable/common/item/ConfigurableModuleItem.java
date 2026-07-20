package com.starmao.scannable.common.item;

import com.starmao.scannable.api.ScannerModule;
import com.starmao.scannable.common.config.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Generic base for configurable scanner module items (block and entity).
 *
 * <p>Handles the common pattern of storing a list of {@link ResourceLocation} IDs
 * in a data component, with a lock flag, tooltip display, and a config GUI.
 *
 * @param <T> the registry type (e.g. {@link net.minecraft.world.level.block.Block})
 */
public abstract class ConfigurableModuleItem<T> extends ScannerModuleItem {
    @FunctionalInterface
    protected interface MenuFactory {
        AbstractContainerMenu create(int id, Inventory inv, InteractionHand hand);
    }

    private final MenuFactory menuFactory;

    protected ConfigurableModuleItem(final ScannerModule module,
                                     final MenuFactory menuFactory) {
        super(module);
        this.menuFactory = menuFactory;
    }

    // ---- Abstract ---- //

    /** The data component key that stores the resource location list. */
    protected abstract DataComponentType<List<ResourceLocation>> getComponent();

    /** The registry used to look up items (e.g. {@link BuiltInRegistries#BLOCK}). */
    protected abstract Registry<T> getRegistry();

    /** Convert a registry key to a human-readable name for tooltips. */
    protected abstract Component getDisplayName(T value);

    /** Tooltip list caption (e.g. "Configured blocks:"). */
    protected abstract Component getListCaption();

    // ---- Locking ---- //

    public boolean isLocked(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LOCKED.get(), false);
    }

    // ---- List management ---- //

    public List<ResourceLocation> getIds(final ItemStack stack) {
        final List<ResourceLocation> ids = stack.get(getComponent());
        return ids != null ? ids : Collections.emptyList();
    }

    public List<T> getValues(final ItemStack stack) {
        final List<ResourceLocation> ids = getIds(stack);
        if (ids.isEmpty()) return Collections.emptyList();
        final List<T> result = new ArrayList<>();
        final Registry<T> registry = getRegistry();
        for (final ResourceLocation id : ids) {
            registry.getOptional(id).ifPresent(result::add);
        }
        return result;
    }

    public boolean addValue(final ItemStack stack, final T value) {
        final Optional<ResourceKey<T>> key = getRegistry().getResourceKey(value);
        if (key.isEmpty()) return false;
        if (isLocked(stack)) return false;

        final ResourceLocation id = key.get().location();
        final List<ResourceLocation> list = new ArrayList<>(getIds(stack));
        if (list.contains(id)) return true;
        if (list.size() >= Constants.CONFIGURABLE_MODULE_SLOTS) return false;

        list.add(id);
        stack.set(getComponent(), List.copyOf(list));
        return true;
    }

    public void setValueAt(final ItemStack stack, final int index, final T value) {
        if (index < 0 || index >= Constants.CONFIGURABLE_MODULE_SLOTS) return;
        final Optional<ResourceKey<T>> key = getRegistry().getResourceKey(value);
        if (key.isEmpty()) return;
        if (isLocked(stack)) return;

        final ResourceLocation id = key.get().location();
        final List<ResourceLocation> list = new ArrayList<>(getIds(stack));

        // Remove existing occurrence first, then insert at target position
        list.remove(id);
        final int insertAt = Math.min(index, list.size());
        list.add(insertAt, id);

        stack.set(getComponent(), List.copyOf(list));
    }

    public void removeValueAt(final ItemStack stack, final int index) {
        if (index < 0 || index >= Constants.CONFIGURABLE_MODULE_SLOTS) return;
        if (isLocked(stack)) return;

        final List<ResourceLocation> list = new ArrayList<>(getIds(stack));
        if (index < list.size()) {
            list.remove(index);
            stack.set(getComponent(), List.copyOf(list));
        }
    }

    // ---- Item overrides ---- //

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        final List<T> values = getValues(stack);
        if (!values.isEmpty()) {
            tooltip.add(getListCaption());
            for (final T value : values) {
                tooltip.add(Component.literal(" - ").append(getDisplayName(value)).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Override
                public AbstractContainerMenu createMenu(final int id, final Inventory inv, final Player player) {
                    return menuFactory.create(id, inv, hand);
                }
            }, buf -> buf.writeEnum(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(final ItemStack stack, final Player player, final LivingEntity target, final InteractionHand hand) {
        // Subclasses override this for entity-type interaction
        return InteractionResult.PASS;
    }
}
