package com.starmao.scannable.common.network.message;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.container.AbstractModuleContainerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S payload: set a configured target at a specific index in a module's list.
 * <p>Sent when the player clicks an empty slot (or uses JEI ghost-drag) in
 * the config GUI. The server resolves the registry name and updates the item.
 */
public record SetConfiguredModuleItemAtMessage(int windowId, int index, ResourceLocation value) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, "set_module_item");
    public static final Type<SetConfiguredModuleItemAtMessage> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SetConfiguredModuleItemAtMessage> STREAM_CODEC =
            StreamCodec.ofMember(
                    (msg, buf) -> {
                        buf.writeVarInt(msg.windowId);
                        buf.writeVarInt(msg.index);
                        buf.writeResourceLocation(msg.value);
                    },
                    buf -> new SetConfiguredModuleItemAtMessage(
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readResourceLocation()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetConfiguredModuleItemAtMessage msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        com.starmao.scannable.common.network.handler.ModuleContainerMenuHandler.handleSet(msg.windowId, msg.index, msg.value, ctx);
    }
}
