package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Tells a player-skeleton or placed-model anchor entity to stop and return to its rest pose. */
public record EntityAnimationStopPacket(UUID entityId) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(entityId.getMostSignificantBits());
            out.writeLong(entityId.getLeastSignificantBits());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static EntityAnimationStopPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            UUID entityId = new UUID(in.readLong(), in.readLong());
            return new EntityAnimationStopPacket(entityId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
