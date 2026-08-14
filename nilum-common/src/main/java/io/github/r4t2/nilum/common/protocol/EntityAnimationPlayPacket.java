package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Tells a player-skeleton or placed-model anchor entity to start playing a named animation. */
public record EntityAnimationPlayPacket(UUID entityId, String animationName, long startTimeMillis) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(entityId.getMostSignificantBits());
            out.writeLong(entityId.getLeastSignificantBits());
            out.writeUTF(animationName);
            out.writeLong(startTimeMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static EntityAnimationPlayPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            UUID entityId = new UUID(in.readLong(), in.readLong());
            String animationName = in.readUTF();
            long startTimeMillis = in.readLong();
            return new EntityAnimationPlayPacket(entityId, animationName, startTimeMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
