package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Client -> server: the player clicked a button element in the custom UI they have open. */
public record UiButtonClickedPacket(String uiId, String elementId) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(uiId);
            out.writeUTF(elementId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static UiButtonClickedPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new UiButtonClickedPacket(in.readUTF(), in.readUTF());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
