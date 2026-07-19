package com.starmao.scannable.common.network.message;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S payload: remove a configured target from a configurable module's list.
 * <p>Sent when the player clicks a filled slot in the config GUI to clear it.
 * The server validates that the player's open container is an
 * {@link AbstractModuleContainerMenu} before modifying the item.
 */
public record RemoveConfiguredModuleItemAtMessage(int windowId, int index) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, "remove_module_item");
    public static final Type<RemoveConfiguredModuleItemAtMessage> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveConfiguredModuleItemAtMessage> STREAM_CODEC =
            StreamCodec.ofMember(
                    (msg, buf) -> {
                        buf.writeVarInt(msg.windowId);
                        buf.writeVarInt(msg.index);
                    },
                    buf -> new RemoveConfiguredModuleItemAtMessage(
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveConfiguredModuleItemAtMessage msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player &&
                player.containerMenu != null &&
                player.containerMenu.containerId == msg.windowId &&
                player.containerMenu instanceof AbstractModuleContainerMenu) {
                ((AbstractModuleContainerMenu) player.containerMenu).removeItemAt(msg.index);
            }
        });
    }
}
