package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Temporarily seizes control of a normally auto element. durationTicks -1 holds until a release. */
public record HudFrameOverridePacket(String atlasId, String elementId, int frame, int durationTicks) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(atlasId);
            out.writeUTF(elementId);
            out.writeInt(frame);
            out.writeInt(durationTicks);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static HudFrameOverridePacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new HudFrameOverridePacket(in.readUTF(), in.readUTF(), in.readInt(), in.readInt());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
