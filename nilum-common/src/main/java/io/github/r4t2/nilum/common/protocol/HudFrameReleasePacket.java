package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Hands an auto element back after a HudFrameOverridePacket. */
public record HudFrameReleasePacket(String atlasId, String elementId) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(atlasId);
            out.writeUTF(elementId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static HudFrameReleasePacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new HudFrameReleasePacket(in.readUTF(), in.readUTF());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
