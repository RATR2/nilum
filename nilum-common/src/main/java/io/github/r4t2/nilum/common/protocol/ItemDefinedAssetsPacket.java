package io.github.r4t2.nilum.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Server -> client: preview data (name, lore, hide_groups) for every loaded model/icon that has a real item definition, for the client's own creative tab. */
public record ItemDefinedAssetsPacket(List<ItemPreviewEntry> models, List<ItemPreviewEntry> icons) {

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            writeEntries(out, models);
            writeEntries(out, icons);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static ItemDefinedAssetsPacket decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return new ItemDefinedAssetsPacket(readEntries(in), readEntries(in));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeEntries(DataOutputStream out, List<ItemPreviewEntry> entries) throws IOException {
        out.writeInt(entries.size());
        for (ItemPreviewEntry entry : entries) {
            out.writeUTF(entry.assetId());
            out.writeUTF(entry.displayName());
            writeStrings(out, entry.lore());
            writeStrings(out, entry.hideGroups());
        }
    }

    private static List<ItemPreviewEntry> readEntries(DataInputStream in) throws IOException {
        int count = in.readInt();
        List<ItemPreviewEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String assetId = in.readUTF();
            String displayName = in.readUTF();
            List<String> lore = readStrings(in);
            List<String> hideGroups = readStrings(in);
            entries.add(new ItemPreviewEntry(assetId, displayName, lore, hideGroups));
        }
        return entries;
    }

    private static void writeStrings(DataOutputStream out, List<String> values) throws IOException {
        out.writeInt(values.size());
        for (String value : values) {
            out.writeUTF(value);
        }
    }

    private static List<String> readStrings(DataInputStream in) throws IOException {
        int count = in.readInt();
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(in.readUTF());
        }
        return values;
    }
}
