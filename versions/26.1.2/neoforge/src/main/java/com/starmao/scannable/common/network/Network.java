package com.starmao.scannable.common.network;

import com.starmao.scannable.common.network.message.RemoveConfiguredModuleItemAtMessage;
import com.starmao.scannable.common.network.message.S2CItemScanResult;
import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network payload registration and convenience senders.
 * <p>Registers all custom packet payloads (C2S and S2C) during mod
 * initialisation using NeoForge's {@link PayloadRegistrar}.
 * Also provides static helper methods for sending configure-module messages.
 */
public final class Network {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Network::registerPayloads);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Scannable.MOD_ID);
        registrar.playToServer(
                SetConfiguredModuleItemAtMessage.TYPE,
                SetConfiguredModuleItemAtMessage.STREAM_CODEC,
                SetConfiguredModuleItemAtMessage::handle
        );
        registrar.playToServer(
                RemoveConfiguredModuleItemAtMessage.TYPE,
                RemoveConfiguredModuleItemAtMessage.STREAM_CODEC,
                RemoveConfiguredModuleItemAtMessage::handle
        );
        registrar.playToClient(
                S2CItemScanResult.TYPE,
                S2CItemScanResult.STREAM_CODEC,
                S2CItemScanResult::handle
        );
    }

    /** Sends a set-configured-item message to the server. */
    public static void sendToServer(SetConfiguredModuleItemAtMessage msg) {
        ClientPacketDistributor.sendToServer(msg);
    }

    /** Sends a remove-configured-item message to the server. */
    public static void sendToServer(RemoveConfiguredModuleItemAtMessage msg) {
        ClientPacketDistributor.sendToServer(msg);
    }

    private Network() {
    }
}
