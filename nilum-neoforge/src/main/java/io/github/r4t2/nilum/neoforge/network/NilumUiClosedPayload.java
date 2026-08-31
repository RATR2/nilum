package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw UiClosedPacket. See NilumModelSpawnPayload. */
public record NilumUiClosedPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumUiClosedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.UI_CLOSED));

    public static final StreamCodec<ByteBuf, NilumUiClosedPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumUiClosedPayload::new, NilumUiClosedPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
