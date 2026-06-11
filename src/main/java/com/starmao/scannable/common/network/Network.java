package com.starmao.scannable.common.network;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.network.message.C2SItemScanRequest;
import com.starmao.scannable.common.network.message.RemoveConfiguredModuleItemAtMessage;
import com.starmao.scannable.common.network.message.S2CItemScanResult;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
        registrar.playToServer(
                C2SItemScanRequest.TYPE,
                C2SItemScanRequest.STREAM_CODEC,
                C2SItemScanRequest::handle
        );
        registrar.playToClient(
                S2CItemScanResult.TYPE,
                S2CItemScanResult.STREAM_CODEC,
                S2CItemScanResult::handle
        );
    }

    public static void sendToServer(SetConfiguredModuleItemAtMessage msg) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(msg);
    }

    public static void sendToServer(RemoveConfiguredModuleItemAtMessage msg) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(msg);
    }

    private Network() {
    }
}
