package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Rewrites one frame's actual pixels; client applies via a sub-region GPU upload, not a full re-upload. */
public record AtlasPatchPacket(String atlasId, String elementId, int frame, byte[] png) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(atlasId);
            out.writeUTF(elementId);
            out.writeInt(frame);
            out.writeInt(png.length);
            out.write(png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static AtlasPatchPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String atlasId = in.readUTF();
            String elementId = in.readUTF();
            int frame = in.readInt();
            byte[] png = new byte[in.readInt()];
            in.readFully(png);
            return new AtlasPatchPacket(atlasId, elementId, frame, png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
