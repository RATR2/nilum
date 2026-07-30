package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw {@code AssetManifestPacket}. See {@link NilumHelloPayload}. */
public record NilumAssetManifestPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumAssetManifestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ASSET_MANIFEST));

    public static final StreamCodec<ByteBuf, NilumAssetManifestPayload> CODEC =
            ByteBufCodecs.BYTE_ARRAY.map(NilumAssetManifestPayload::new, NilumAssetManifestPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
