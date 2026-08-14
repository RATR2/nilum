package io.github.r4t2.nilum.neoforge.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Reads/writes a byte array as the entire packet remainder, no length prefix, matching Paper's raw plugin-message wire format. */
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
