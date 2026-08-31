package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw OpenUiPacket. See NilumModelSpawnPayload. */
public record NilumOpenUiPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumOpenUiPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.OPEN_UI));

    public static final StreamCodec<ByteBuf, NilumOpenUiPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumOpenUiPayload::new, NilumOpenUiPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
