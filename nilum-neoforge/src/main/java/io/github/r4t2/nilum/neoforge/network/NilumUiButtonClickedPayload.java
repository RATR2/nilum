package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw UiButtonClickedPacket. See NilumModelSpawnPayload. */
public record NilumUiButtonClickedPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumUiButtonClickedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.UI_BUTTON_CLICKED));

    public static final StreamCodec<ByteBuf, NilumUiButtonClickedPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumUiButtonClickedPayload::new, NilumUiButtonClickedPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
