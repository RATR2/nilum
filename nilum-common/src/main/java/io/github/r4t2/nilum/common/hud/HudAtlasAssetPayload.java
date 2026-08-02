package io.github.r4t2.nilum.common.hud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * What travels over the wire for a HUD atlas: the spritesheet PNG bytes plus the raw .atlas
 * descriptor JSON bytes, bundled into one payload so the existing manifest/TCP asset-fetch
 * transport can carry both.
 */
public record HudAtlasAssetPayload(byte[] pngBytes, byte[] descriptorJsonBytes) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(pngBytes.length);
            out.write(pngBytes);
            out.writeInt(descriptorJsonBytes.length);
            out.write(descriptorJsonBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static HudAtlasAssetPayload decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            byte[] pngBytes = new byte[in.readInt()];
            in.readFully(pngBytes);
            byte[] descriptorJsonBytes = new byte[in.readInt()];
            in.readFully(descriptorJsonBytes);
            return new HudAtlasAssetPayload(pngBytes, descriptorJsonBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public HudAtlasDescriptor decodeDescriptor() {
        return HudAtlasParser.parse(new String(descriptorJsonBytes, StandardCharsets.UTF_8));
    }
}
