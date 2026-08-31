package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Server -> client: shows or hides an entire HUD atlas for this player. */
public record SetHudAtlasVisibilityPacket(String atlasId, boolean visible) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(atlasId);
            out.writeBoolean(visible);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static SetHudAtlasVisibilityPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new SetHudAtlasVisibilityPacket(in.readUTF(), in.readBoolean());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
