package io.github.r4t2.nilum.fabric.font;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves a streamed .ttf to <config>/nilum/font/<id>.ttf. Step 1 of custom font support; actually using it in rendering comes next. */
public final class FontInstaller {

    private FontInstaller() {
    }

    public static void install(String fontId, byte[] ttfBytes, Path fontDirectory) {
        try {
            Files.createDirectories(fontDirectory);
            Files.write(fontDirectory.resolve(fontId + ".ttf"), ttfBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to install font '" + fontId + "'", e);
        }
    }
}
