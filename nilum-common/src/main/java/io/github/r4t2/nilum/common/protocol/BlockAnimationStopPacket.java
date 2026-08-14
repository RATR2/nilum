package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Tells a custom block at x,y,z (in the client's current level) to stop and return to its rest pose. */
public record BlockAnimationStopPacket(int x, int y, int z) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static BlockAnimationStopPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new BlockAnimationStopPacket(in.readInt(), in.readInt(), in.readInt());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
