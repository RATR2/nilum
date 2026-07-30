package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public record TcpOfferPacket(String host, int port, String token) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(host);
            out.writeInt(port);
            out.writeUTF(token);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static TcpOfferPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String host = in.readUTF();
            int port = in.readInt();
            String token = in.readUTF();
            return new TcpOfferPacket(host, port, token);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
