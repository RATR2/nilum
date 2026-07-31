package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public record AssetRequestPacket(String assetId, AssetKind kind) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(assetId);
            out.writeByte(kind.ordinal());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static AssetRequestPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String assetId = in.readUTF();
            AssetKind kind = AssetKind.values()[in.readByte()];
            return new AssetRequestPacket(assetId, kind);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
