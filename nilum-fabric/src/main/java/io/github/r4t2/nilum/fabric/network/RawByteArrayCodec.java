package io.github.r4t2.nilum.fabric.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Reads/writes a byte array as the entire remainder of the packet, with no
 * length prefix - matches Paper's raw plugin-message wire format. Unlike
 * {@code ByteBufCodecs.BYTE_ARRAY}, which prefixes a VarInt length and is
 * only wire-compatible when both ends are Fabric.
 */
final class RawByteArrayCodec {

    static final StreamCodec<ByteBuf, byte[]> INSTANCE = StreamCodec.of(
            (buf, data) -> buf.writeBytes(data),
            buf -> {
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return data;
            });

    private RawByteArrayCodec() {
    }
}
