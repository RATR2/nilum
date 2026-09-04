package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw ItemDefinedAssetsPacket. See NilumModelSpawnPayload. */
public record NilumItemDefinedAssetsPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumItemDefinedAssetsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ITEM_DEFINED_ASSETS));

    public static final StreamCodec<ByteBuf, NilumItemDefinedAssetsPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumItemDefinedAssetsPayload::new, NilumItemDefinedAssetsPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
