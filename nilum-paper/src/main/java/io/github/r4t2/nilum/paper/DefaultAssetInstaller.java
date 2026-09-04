package io.github.r4t2.nilum.paper;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts bundled starter content (models/blocks/items/hud/ui/textures/...) from the plugin's own
 * jar into the data folder, skipping anything already there. Adding a new default asset later is
 * just adding a file under the matching folder in src/main/resources, no code change needed.
 */
final class DefaultAssetInstaller {

    private static final Set<String> BUNDLED_FOLDERS = Set.of(
            "models/", "blocks/", "items/", "icons/", "textures/", "hud/", "ui/", "shaderpacks/", "fonts/");

    private DefaultAssetInstaller() {
    }

    static void install(JavaPlugin plugin, NilumLogger logger) {
        Path jarPath;
        try {
            jarPath = Path.of(DefaultAssetInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            logger.warn("Couldn't locate the plugin jar to install default assets from", e);
            return;
        }

        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName)
                    .filter(DefaultAssetInstaller::isBundledDefault)
                    .forEach(resourcePath -> extractIfMissing(plugin, logger, resourcePath));
        } catch (IOException e) {
            logger.warn("Couldn't read the plugin jar to install default assets", e);
        }
    }

    private static boolean isBundledDefault(String resourcePath) {
        return BUNDLED_FOLDERS.stream().anyMatch(resourcePath::startsWith);
    }

    private static void extractIfMissing(JavaPlugin plugin, NilumLogger logger, String resourcePath) {
        Path target = plugin.getDataFolder().toPath().resolve(resourcePath);
        if (Files.exists(target)) {
            return;
        }

        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            logger.info("Installed default asset '" + resourcePath + "'.");
        } catch (IOException e) {
            logger.warn("Failed to install default asset '" + resourcePath + "'", e);
        }
    }
}
