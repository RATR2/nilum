package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw EntityAnimationStopPacket. See NilumHelloPayload. */
public record NilumEntityAnimationStopPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumEntityAnimationStopPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ENTITY_ANIMATION_STOP));

    public static final StreamCodec<ByteBuf, NilumEntityAnimationStopPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumEntityAnimationStopPayload::new, NilumEntityAnimationStopPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
