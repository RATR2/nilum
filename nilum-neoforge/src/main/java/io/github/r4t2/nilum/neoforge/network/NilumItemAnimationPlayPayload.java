package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw packet. See NilumModelSpawnPayload. */
public record NilumItemAnimationPlayPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumItemAnimationPlayPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ITEM_ANIMATION_PLAY));

    public static final StreamCodec<ByteBuf, NilumItemAnimationPlayPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumItemAnimationPlayPayload::new, NilumItemAnimationPlayPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
