package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Tells a held item in a player's visual left/right hand to stop and return to its rest pose. */
public record ItemAnimationStopPacket(UUID holderId, boolean rightHand) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(holderId.getMostSignificantBits());
            out.writeLong(holderId.getLeastSignificantBits());
            out.writeBoolean(rightHand);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static ItemAnimationStopPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            UUID holderId = new UUID(in.readLong(), in.readLong());
            boolean rightHand = in.readBoolean();
            return new ItemAnimationStopPacket(holderId, rightHand);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
