package io.github.r4t2.nilum.common.hud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What travels over the wire for a HUD atlas: the .atlas descriptor bytes, an optional shared
 * spritesheet, and every distinct PNG an Image element references, bundled into one payload.
 */
public record HudAtlasAssetPayload(Optional<byte[]> spritesheetPngBytes, Map<String, byte[]> imageTextures, byte[] descriptorBytes) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeBoolean(spritesheetPngBytes.isPresent());
            if (spritesheetPngBytes.isPresent()) {
                byte[] sheet = spritesheetPngBytes.get();
                out.writeInt(sheet.length);
                out.write(sheet);
            }

            out.writeInt(imageTextures.size());
            for (Map.Entry<String, byte[]> entry : imageTextures.entrySet()) {
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

    public static HudAtlasAssetPayload decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Optional<byte[]> spritesheet = Optional.empty();
            if (in.readBoolean()) {
                byte[] sheet = new byte[in.readInt()];
                in.readFully(sheet);
                spritesheet = Optional.of(sheet);
            }

            int textureCount = in.readInt();
            Map<String, byte[]> imageTextures = new LinkedHashMap<>();
            for (int i = 0; i < textureCount; i++) {
                String fileName = in.readUTF();
                byte[] png = new byte[in.readInt()];
                in.readFully(png);
                imageTextures.put(fileName, png);
            }

            byte[] descriptorBytes = new byte[in.readInt()];
            in.readFully(descriptorBytes);

            return new HudAtlasAssetPayload(spritesheet, imageTextures, descriptorBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public HudAtlasDescriptor decodeDescriptor() {
        return HudAtlasParser.parse(new String(descriptorBytes, StandardCharsets.UTF_8));
    }
}
