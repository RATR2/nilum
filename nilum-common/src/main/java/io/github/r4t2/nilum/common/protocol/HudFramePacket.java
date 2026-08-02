package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Cheap server -> client frame-index update for a server-type HUD atlas element. */
public record HudFramePacket(String atlasId, String elementId, int frame) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(atlasId);
            out.writeUTF(elementId);
            out.writeInt(frame);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static HudFramePacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new HudFramePacket(in.readUTF(), in.readUTF(), in.readInt());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
