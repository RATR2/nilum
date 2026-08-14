package io.github.r4t2.nilum.paper.texture;

import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Every texture in textures/ gets a companion <name>.yml recording which systems reference it. Best-effort, never breaks a reload. */
public final class TextureUsageTracker {

    private TextureUsageTracker() {
    }

    public static void recordUsage(Path texturesDirectory, String textureFileName, String category) {
        String baseName = stripExtension(textureFileName);
        Path tagFile = texturesDirectory.resolve(baseName + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tagFile.toFile());

        List<String> where = yaml.getStringList("where");
        if (where.contains(category)) {
            return;
        }

        List<String> updated = new ArrayList<>(where);
        updated.add(category);
        yaml.set("where", updated);
        try {
            yaml.save(tagFile.toFile());
        } catch (Exception ignored) {
            // Best-effort bookkeeping, not worth failing a reload over.
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
