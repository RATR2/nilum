package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Client -> server: a Nilum keybind slot changed state. Slot is 0-based, pressed is the new state. */
public record KeybindPacket(int slot, boolean pressed) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(slot);
            out.writeBoolean(pressed);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static KeybindPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new KeybindPacket(in.readByte(), in.readBoolean());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
