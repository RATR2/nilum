package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Server -> client: shows or hides one Custom UI element for this player, per its requirement expression. */
public record SetUiElementVisibilityPacket(String uiId, String elementId, boolean visible) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(uiId);
            out.writeUTF(elementId);
            out.writeBoolean(visible);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static SetUiElementVisibilityPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new SetUiElementVisibilityPacket(in.readUTF(), in.readUTF(), in.readBoolean());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
