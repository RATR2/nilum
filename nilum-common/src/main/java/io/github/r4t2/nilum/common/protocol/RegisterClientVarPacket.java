package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Declares a server-pushed named variable an {@code auto} expression can reference as {@code number("server:name")}. */
public record RegisterClientVarPacket(String name, double initialValue) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(name);
            out.writeDouble(initialValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static RegisterClientVarPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new RegisterClientVarPacket(in.readUTF(), in.readDouble());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
