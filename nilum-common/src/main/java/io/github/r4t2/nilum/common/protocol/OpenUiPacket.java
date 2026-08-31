package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Server -> client: open a custom UI by id. */
public record OpenUiPacket(String uiId) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(uiId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static OpenUiPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new OpenUiPacket(in.readUTF());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
