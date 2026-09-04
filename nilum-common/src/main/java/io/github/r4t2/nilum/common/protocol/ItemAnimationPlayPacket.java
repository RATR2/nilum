package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Tells a held item in a player's visual left/right hand to start playing a named animation. */
public record ItemAnimationPlayPacket(UUID holderId, boolean rightHand, String animationName, long startTimeMillis) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(holderId.getMostSignificantBits());
            out.writeLong(holderId.getLeastSignificantBits());
            out.writeBoolean(rightHand);
            out.writeUTF(animationName);
            out.writeLong(startTimeMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static ItemAnimationPlayPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            UUID holderId = new UUID(in.readLong(), in.readLong());
            boolean rightHand = in.readBoolean();
            String animationName = in.readUTF();
            long startTimeMillis = in.readLong();
            return new ItemAnimationPlayPacket(holderId, rightHand, animationName, startTimeMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
