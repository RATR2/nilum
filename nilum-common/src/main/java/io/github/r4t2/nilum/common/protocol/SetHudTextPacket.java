package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Server -> client: the already-resolved display string for a render_text element's server_connector. */
public record SetHudTextPacket(String atlasId, String elementId, String text) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(atlasId);
            out.writeUTF(elementId);
            out.writeUTF(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static SetHudTextPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new SetHudTextPacket(in.readUTF(), in.readUTF(), in.readUTF());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
