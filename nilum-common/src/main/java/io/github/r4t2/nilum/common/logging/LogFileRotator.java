package io.github.r4t2.nilum.common.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LogFileRotator {

    private LogFileRotator() {
    }

    public static void rotateOnStartup(Path logFile, int keep) {
        if (!Files.exists(logFile)) {
            return;
        }
        try {
            Files.deleteIfExists(sibling(logFile, keep));

            for (int i = keep - 1; i >= 1; i--) {
                Path from = sibling(logFile, i);
                Path to = sibling(logFile, i + 1);
                if (Files.exists(from)) {
                    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Files.move(logFile, sibling(logFile, 1), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Rotation failing isn't fatal, worst case we keep appending to one file.
        }
    }

    private static Path sibling(Path logFile, int index) {
        String name = logFile.getFileName().toString();
        return logFile.resolveSibling(name + "." + index);
    }
}
