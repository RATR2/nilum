package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

public record ModelSpawnPacket(UUID entityId, String modelId) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(entityId.getMostSignificantBits());
            out.writeLong(entityId.getLeastSignificantBits());
            out.writeUTF(modelId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static ModelSpawnPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            UUID entityId = new UUID(in.readLong(), in.readLong());
            String modelId = in.readUTF();
            return new ModelSpawnPacket(entityId, modelId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
