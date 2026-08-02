package io.github.r4t2.nilum.common.icon;

import io.github.r4t2.nilum.common.model.BbVector3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * What travels over the wire for an icon asset: the PNG bytes plus its fully-resolved
 * IconDisplay, bundled into one payload so the existing manifest/TCP asset-fetch transport
 * can carry both without protocol changes.
 */
public record IconAssetPayload(byte[] pngBytes, IconDisplay display) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(pngBytes.length);
            out.write(pngBytes);

            out.writeInt(display.byContext().size());
            for (Map.Entry<String, IconTransform> entry : display.byContext().entrySet()) {
                out.writeUTF(entry.getKey());
                writeVector3(out, entry.getValue().rotation());
                writeVector3(out, entry.getValue().translation());
                writeVector3(out, entry.getValue().scale());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static IconAssetPayload decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            byte[] pngBytes = new byte[in.readInt()];
            in.readFully(pngBytes);

            int contextCount = in.readInt();
            Map<String, IconTransform> byContext = new HashMap<>();
            for (int i = 0; i < contextCount; i++) {
                String context = in.readUTF();
                BbVector3 rotation = readVector3(in);
                BbVector3 translation = readVector3(in);
                BbVector3 scale = readVector3(in);
                byContext.put(context, new IconTransform(rotation, translation, scale));
            }

            return new IconAssetPayload(pngBytes, new IconDisplay(byContext));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeVector3(DataOutputStream out, BbVector3 vector) throws IOException {
        out.writeDouble(vector.x());
        out.writeDouble(vector.y());
        out.writeDouble(vector.z());
    }

    private static BbVector3 readVector3(DataInputStream in) throws IOException {
        return new BbVector3(in.readDouble(), in.readDouble(), in.readDouble());
    }
}
