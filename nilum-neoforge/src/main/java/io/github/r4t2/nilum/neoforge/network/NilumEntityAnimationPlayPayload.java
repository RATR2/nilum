package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw packet. See NilumModelSpawnPayload. */
public record NilumEntityAnimationPlayPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumEntityAnimationPlayPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ENTITY_ANIMATION_PLAY));

    public static final StreamCodec<ByteBuf, NilumEntityAnimationPlayPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumEntityAnimationPlayPayload::new, NilumEntityAnimationPlayPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
