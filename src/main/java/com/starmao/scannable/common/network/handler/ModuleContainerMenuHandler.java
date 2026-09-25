package com.starmao.scannable.common.network.handler;

import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

/**
 * Shared handler logic for C2S payloads targeting {@link AbstractModuleContainerMenu}.
 *
 * <p>Both {@code SetConfiguredModuleItemAtMessage} and
 * {@code RemoveConfiguredModuleItemAtMessage} perform the same validation
 * (player instanceof ServerPlayer, containerId matches, menu instanceof
 * AbstractModuleContainerMenu). This class extracts that pattern into
 * reusable helpers.
 */
public final class ModuleContainerMenuHandler {

    /**
     * Locates the {@link AbstractModuleContainerMenu} matching the given
     * {@code windowId} on the player's open container, or {@code null} if
     * validation fails.
     */
    @Nullable
    private static AbstractModuleContainerMenu findMenu(final IPayloadContext ctx, final int windowId) {
        if (ctx.player() instanceof ServerPlayer player
                && player.containerMenu != null
                && player.containerMenu.containerId == windowId
                && player.containerMenu instanceof final AbstractModuleContainerMenu menu) {
            return menu;
        }
        return null;
    }

    /**
     * Handles a "remove item at index" operation with full validation.
     */
    public static void handleRemove(final int windowId, final int index, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            final AbstractModuleContainerMenu menu = findMenu(ctx, windowId);
            if (menu != null) {
                menu.removeItemAt(index);
            }
        });
    }

    /**
     * Handles a "set item at index" operation with full validation.
     */
    public static void handleSet(final int windowId, final int index, final ResourceLocation value, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            final AbstractModuleContainerMenu menu = findMenu(ctx, windowId);
            if (menu != null) {
                menu.setItemAt(index, value);
            }
        });
    }

    private ModuleContainerMenuHandler() {}
}
