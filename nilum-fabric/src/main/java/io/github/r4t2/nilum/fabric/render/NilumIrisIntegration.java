package io.github.r4t2.nilum.fabric.render;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Iris integration via its real public API: getShaderpacksDirectoryManager to install a pack
 * file, IrisConfig plus loadShaderpackWhenPossible to switch to it. Switching packs makes Iris
 * rebuild its whole rendering pipeline. Every caller MUST check FabricLoader.isModLoaded("iris")
 * first; loading this class without Iris installed throws NoClassDefFoundError.
 */
public final class NilumIrisIntegration {

    private String previousPackName;
    private boolean previousShadersEnabled;
    private boolean hasPrevious;

    /** Installs a pack zip into Iris's real shaderpacks directory under <packId>.zip. */
    public void installPack(String packId, byte[] zipBytes, Path scratchDir) {
        try {
            Files.createDirectories(scratchDir);
            Path staged = scratchDir.resolve(packId + "_staged.zip");
            Files.write(staged, zipBytes);
            try {
                // ShaderpackDirectoryManager.copyPackIntoDirectory uses a plain Files.copy with no
                // REPLACE_EXISTING; it throws FileAlreadyExistsException if anything's already
                // sitting at that name. Clear the target first so this is a real install-or-update.
                Path target = Iris.getShaderpacksDirectory().resolve(packId + ".zip");
                Files.deleteIfExists(target);
                Iris.getShaderpacksDirectoryManager().copyPackIntoDirectory(packId + ".zip", staged);
            } finally {
                Files.deleteIfExists(staged);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to install shader pack '" + packId + "'", e);
        }
    }

    /** Switches Iris to the named pack, remembering whatever was active before the first activation. */
    public void activate(String packId) {
        IrisConfig config = Iris.getIrisConfig();
        if (!hasPrevious) {
            previousPackName = config.getShaderPackName().orElse(null);
            previousShadersEnabled = config.areShadersEnabled();
            hasPrevious = true;
        }
        config.setShaderPackName(packId + ".zip");
        config.setShadersEnabled(true);
        saveAndReload(config);
    }

    /** Restores whatever pack/enabled state was active before the first activate() call. */
    public void deactivate() {
        if (!hasPrevious) {
            return;
        }
        IrisConfig config = Iris.getIrisConfig();
        if (previousPackName != null) {
            config.setShaderPackName(previousPackName);
        }
        config.setShadersEnabled(previousShadersEnabled);
        saveAndReload(config);
        hasPrevious = false;
    }

    private static void saveAndReload(IrisConfig config) {
        try {
            config.save();
            // loadShaderpackWhenPossible() only sets a flag Iris's own handleKeybinds() consumes,
            // and loadShaderpack() alone (tried first) only updates which pack is *configured*;
            // neither actually tears down and rebuilds the active rendering pipeline, which is why
            // the GUI showed the new pack selected while the world kept rendering the old one.
            // reload() is the real full sequence: destroy the current pipeline, load the new
            // pack, rebuild, the same thing /iris reload runs.
            Iris.reload();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
