package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public record AssetManifestPacket(List<AssetManifestEntry> entries) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(entries.size());
            for (AssetManifestEntry entry : entries) {
                out.writeUTF(entry.assetId());
                out.writeUTF(entry.sha256());
                out.writeByte(entry.kind().ordinal());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static AssetManifestPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int count = in.readInt();
            List<AssetManifestEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String assetId = in.readUTF();
                String sha256 = in.readUTF();
                AssetKind kind = AssetKind.values()[in.readByte()];
                entries.add(new AssetManifestEntry(assetId, sha256, kind));
            }
            return new AssetManifestPacket(entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
