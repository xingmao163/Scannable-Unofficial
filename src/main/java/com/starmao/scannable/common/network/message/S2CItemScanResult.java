package com.starmao.scannable.common.network.message;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.api.ClientScanHandler;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import com.starmao.scannable.common.util.ClientAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client scan result for the item scanner module.
 *
 * <p>The server sends this after scanning completes (see {@code ScannerItem.finishScanServer()}).
 * The client receives the results and passes them to {@code ScanManager.setServerItemResults()}
 * for rendering.
 */
public record S2CItemScanResult(Vec3 center, List<ItemScanResultData> results) implements CustomPacketPayload {

    /**
     * Upper bound on the number of results accepted from the network.
     *
     * <p>A scan may legitimately report many containers, but the decoder pre-allocates a list
     * from the wire-supplied count, so the value has to be bounded.
     */
    private static final int MAX_RESULTS = 8192;

    static final Identifier ID = Identifier.fromNamespaceAndPath(Scannable.MOD_ID, "s2c_item_scan");

    public static final Type<S2CItemScanResult> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CItemScanResult> STREAM_CODEC =
            StreamCodec.ofMember(
                    (msg, buf) -> {
                        buf.writeDouble(msg.center.x);
                        buf.writeDouble(msg.center.y);
                        buf.writeDouble(msg.center.z);
                        buf.writeVarInt(msg.results.size());
                        for (final ItemScanResultData r : msg.results) {
                            buf.writeBlockPos(r.pos());
                            buf.writeIdentifier(r.itemId());
                            buf.writeVarInt(r.totalCount());
                        }
                    },
                    buf -> {
                        final Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                        final int count = buf.readVarInt();
                        // Bound the pre-allocation: a corrupt or hostile packet could otherwise
                        // make the client allocate an arbitrarily large list before reading it.
                        if (count < 0 || count > MAX_RESULTS) {
                            throw new io.netty.handler.codec.DecoderException(
                                    "S2CItemScanResult result count out of range: " + count);
                        }
                        final List<ItemScanResultData> results = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            final BlockPos pos = buf.readBlockPos();
                            final Identifier itemId = buf.readIdentifier();
                            final int totalCount = buf.readVarInt();
                            results.add(new ItemScanResultData(pos, itemId, totalCount));
                        }
                        return new S2CItemScanResult(center, results);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ---- Handler (Client Side) ---- //

    public static void handle(final S2CItemScanResult msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Resolve the client handler first: it is the only thing this packet needs, and
            // touching ctx.player() before knowing we can act risks an NPE during the
            // configuration phase or a disconnect race.
            final ClientScanHandler h = ClientAccessor.getHandler();
            if (h == null) return;
            if (!ctx.player().level().isClientSide()) return;
            if (ServerConfig.DEBUG_LOG_ITEM_SCANNER.get()) {
                Scannable.LOGGER.info("[ItemScanner] Received {} server scan result(s)", msg.results.size());
            }
            h.setServerItemResults(msg.center(), msg.results());
        });
    }
}
