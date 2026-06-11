package com.starmao.scannable.network;

import com.starmao.scannable.client.ScanManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ScanResultsPayload(Vec3 center, List<ScanResultEntry> results) implements CustomPacketPayload {
    public static final ResourceLocation ID = com.starmao.scannable.Scannable.id("scan_results");
    public static final Type<ScanResultsPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanResultsPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ScanResultsPayload decode(RegistryFriendlyByteBuf buf) {
                    Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    int count = buf.readVarInt();
                    List<ScanResultEntry> results = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        BlockPos pos = buf.readBlockPos();
                        String displayName = buf.readUtf();
                        int remaining = buf.readVarInt();
                        String oreType = buf.readUtf();
                        results.add(new ScanResultEntry(pos, displayName, remaining, oreType));
                    }
                    return new ScanResultsPayload(center, results);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ScanResultsPayload value) {
                    buf.writeDouble(value.center.x);
                    buf.writeDouble(value.center.y);
                    buf.writeDouble(value.center.z);
                    buf.writeVarInt(value.results.size());
                    for (ScanResultEntry entry : value.results) {
                        buf.writeBlockPos(entry.pos());
                        buf.writeUtf(entry.displayName());
                        buf.writeVarInt(entry.remainingBlocks());
                        buf.writeUtf(entry.oreType());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScanResultsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().isClientSide()) {
                // ScanResults are handled by provider system(payload.center(), payload.results());
            }
        });
    }
}
