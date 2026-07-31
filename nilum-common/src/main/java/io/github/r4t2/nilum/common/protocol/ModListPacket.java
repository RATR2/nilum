package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public record ModListPacket(List<ModEntry> mods) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(mods.size());
            for (ModEntry mod : mods) {
                out.writeUTF(mod.modId());
                out.writeUTF(mod.version());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static ModListPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int count = in.readInt();
            List<ModEntry> mods = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String modId = in.readUTF();
                String version = in.readUTF();
                mods.add(new ModEntry(modId, version));
            }
            return new ModListPacket(mods);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
