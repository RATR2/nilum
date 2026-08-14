package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Tells a custom block at x,y,z (in the client's current level) to start playing a named animation. */
public record BlockAnimationPlayPacket(int x, int y, int z, String animationName, long startTimeMillis) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);
            out.writeUTF(animationName);
            out.writeLong(startTimeMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static BlockAnimationPlayPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();
            String animationName = in.readUTF();
            long startTimeMillis = in.readLong();
            return new BlockAnimationPlayPacket(x, y, z, animationName, startTimeMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
