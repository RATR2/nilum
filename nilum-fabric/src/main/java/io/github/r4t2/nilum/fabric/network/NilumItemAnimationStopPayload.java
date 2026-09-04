package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw ItemAnimationStopPacket. See NilumHelloPayload. */
public record NilumItemAnimationStopPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumItemAnimationStopPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ITEM_ANIMATION_STOP));

    public static final StreamCodec<ByteBuf, NilumItemAnimationStopPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumItemAnimationStopPayload::new, NilumItemAnimationStopPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
