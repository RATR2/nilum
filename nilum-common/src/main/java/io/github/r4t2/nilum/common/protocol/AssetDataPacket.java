package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public record AssetDataPacket(String assetId, AssetKind kind, boolean found, byte[] data) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(assetId);
            out.writeByte(kind.ordinal());
            out.writeBoolean(found);
            out.writeInt(data.length);
            out.write(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static AssetDataPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String assetId = in.readUTF();
            AssetKind kind = AssetKind.values()[in.readByte()];
            boolean found = in.readBoolean();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            return new AssetDataPacket(assetId, kind, found, data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
