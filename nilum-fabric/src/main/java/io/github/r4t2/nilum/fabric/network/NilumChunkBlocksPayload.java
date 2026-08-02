package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw ChunkBlocksPacket. See NilumHelloPayload. */
public record NilumChunkBlocksPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumChunkBlocksPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.CHUNK_BLOCKS));

    public static final StreamCodec<ByteBuf, NilumChunkBlocksPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumChunkBlocksPayload::new, NilumChunkBlocksPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
