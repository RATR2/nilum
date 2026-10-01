package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw packet. See NilumModelSpawnPayload. */
public record NilumSetUiElementVisibilityPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumSetUiElementVisibilityPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.SET_UI_ELEMENT_VISIBILITY));

    public static final StreamCodec<ByteBuf, NilumSetUiElementVisibilityPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumSetUiElementVisibilityPayload::new, NilumSetUiElementVisibilityPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
