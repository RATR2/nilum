package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw RegisterClientVarPacket. See NilumHelloPayload. */
public record NilumRegisterClientVarPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumRegisterClientVarPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.REGISTER_CLIENT_VAR));

    public static final StreamCodec<ByteBuf, NilumRegisterClientVarPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumRegisterClientVarPayload::new, NilumRegisterClientVarPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
