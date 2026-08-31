package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw SetHudElementVisibilityPacket. See NilumHelloPayload. */
public record NilumSetHudElementVisibilityPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumSetHudElementVisibilityPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.SET_HUD_ELEMENT_VISIBILITY));

    public static final StreamCodec<ByteBuf, NilumSetHudElementVisibilityPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumSetHudElementVisibilityPayload::new, NilumSetHudElementVisibilityPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
