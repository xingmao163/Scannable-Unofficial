package com.starmao.scannable.common.network.message;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client scan result for the item scanner module.
 *
 * <p>The server sends this after processing a {@link C2SItemScanRequest}.
 * The client receives the results and passes them to the scan result
 * provider for rendering.
 */
public record S2CItemScanResult(Vec3 center, List<ItemScanResultData> results) implements CustomPacketPayload {
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, "s2c_item_scan");
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
                            buf.writeResourceLocation(r.itemId());
                            buf.writeVarInt(r.totalCount());
                        }
                    },
                    buf -> {
                        final Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                        final int count = buf.readVarInt();
                        final List<ItemScanResultData> results = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            final BlockPos pos = buf.readBlockPos();
                            final ResourceLocation itemId = buf.readResourceLocation();
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
            if (!ctx.player().level().isClientSide()) return;
            Scannable.LOGGER.info("[ItemScanner] Received {} server scan result(s)", msg.results.size());
            com.starmao.scannable.client.ScanManager.setServerItemResults(msg.center, msg.results);
        });
    }
}
