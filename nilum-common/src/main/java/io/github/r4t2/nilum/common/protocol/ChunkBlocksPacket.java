package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Server -> client: which positions in one chunk are Nilum custom blocks. Also reused for live single-block placement/removal. */
public record ChunkBlocksPacket(int chunkX, int chunkZ, List<ChunkBlockEntry> entries) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(chunkX);
            out.writeInt(chunkZ);
            out.writeInt(entries.size());
            for (ChunkBlockEntry entry : entries) {
                out.writeInt(entry.x());
                out.writeInt(entry.y());
                out.writeInt(entry.z());
                boolean hasModel = entry.modelId() != null;
                out.writeBoolean(hasModel);
                if (hasModel) {
                    out.writeUTF(entry.modelId());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static ChunkBlocksPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int chunkX = in.readInt();
            int chunkZ = in.readInt();
            int count = in.readInt();
            List<ChunkBlockEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                String modelId = in.readBoolean() ? in.readUTF() : null;
                entries.add(new ChunkBlockEntry(x, y, z, modelId));
            }
            return new ChunkBlocksPacket(chunkX, chunkZ, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
