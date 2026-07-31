package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw {@code ModelSpawnPacket}. See {@link NilumHelloPayload}. */
public record NilumModelSpawnPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumModelSpawnPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.MODEL_SPAWN));

    public static final StreamCodec<ByteBuf, NilumModelSpawnPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumModelSpawnPayload::new, NilumModelSpawnPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
