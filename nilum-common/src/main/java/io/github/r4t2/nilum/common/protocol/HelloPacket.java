package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public record HelloPacket(String protocolVersion, String serverModVersion) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(protocolVersion);
            out.writeUTF(serverModVersion);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static HelloPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String protocolVersion = in.readUTF();
            String serverModVersion = in.readUTF();
            return new HelloPacket(protocolVersion, serverModVersion);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
