package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public record HelloAckPacket(
        String loader,
        String modVersion,
        String protocolVersion,
        boolean supportsShaders,
        boolean supportsTcp,
        String shaderRenderer
) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(loader);
            out.writeUTF(modVersion);
            out.writeUTF(protocolVersion);
            out.writeBoolean(supportsShaders);
            out.writeBoolean(supportsTcp);
            out.writeUTF(shaderRenderer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static HelloAckPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String loader = in.readUTF();
            String modVersion = in.readUTF();
            String protocolVersion = in.readUTF();
            boolean supportsShaders = in.readBoolean();
            boolean supportsTcp = in.readBoolean();
            String shaderRenderer = in.readUTF();
            return new HelloAckPacket(loader, modVersion, protocolVersion, supportsShaders, supportsTcp, shaderRenderer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
