package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw ActivateShaderPackPacket. See NilumHelloPayload. */
public record NilumActivateShaderPackPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumActivateShaderPackPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ACTIVATE_SHADER_PACK));

    public static final StreamCodec<ByteBuf, NilumActivateShaderPackPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumActivateShaderPackPayload::new, NilumActivateShaderPackPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
