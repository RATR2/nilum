package io.github.r4t2.nilum.common.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** What travels over the wire for a custom UI: the .ui descriptor bytes plus every image its elements reference. */
public record UiAssetPayload(Map<String, byte[]> images, byte[] descriptorBytes) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(images.size());
            for (Map.Entry<String, byte[]> entry : images.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeInt(entry.getValue().length);
                out.write(entry.getValue());
            }

            out.writeInt(descriptorBytes.length);
            out.write(descriptorBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static UiAssetPayload decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int imageCount = in.readInt();
            Map<String, byte[]> images = new LinkedHashMap<>();
            for (int i = 0; i < imageCount; i++) {
                String fileName = in.readUTF();
                byte[] png = new byte[in.readInt()];
                in.readFully(png);
                images.put(fileName, png);
            }

            byte[] descriptorBytes = new byte[in.readInt()];
            in.readFully(descriptorBytes);

            return new UiAssetPayload(images, descriptorBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public UiDescriptor decodeDescriptor() {
        return UiParser.parse(new String(descriptorBytes, StandardCharsets.UTF_8));
    }
}
