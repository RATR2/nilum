package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw OpenChestUiPacket. See NilumHelloPayload. */
public record NilumOpenChestUiPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumOpenChestUiPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.OPEN_CHEST_UI));

    public static final StreamCodec<ByteBuf, NilumOpenChestUiPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumOpenChestUiPayload::new, NilumOpenChestUiPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
