package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw BlockAnimationStopPacket. See NilumHelloPayload. */
public record NilumBlockAnimationStopPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumBlockAnimationStopPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.BLOCK_ANIMATION_STOP));

    public static final StreamCodec<ByteBuf, NilumBlockAnimationStopPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumBlockAnimationStopPayload::new, NilumBlockAnimationStopPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
