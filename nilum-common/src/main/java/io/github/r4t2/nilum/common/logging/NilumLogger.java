package io.github.r4t2.nilum.common.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BooleanSupplier;

public final class NilumLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NilumLogSink sink;
    private final Path logFile;
    private final BooleanSupplier debugEnabled;

    public NilumLogger(NilumLogSink sink, Path logFile, BooleanSupplier debugEnabled, int maxLogFiles) {
        this.sink = sink;
        this.logFile = logFile;
        this.debugEnabled = debugEnabled;
        if (logFile != null) {
            LogFileRotator.rotateOnStartup(logFile, maxLogFiles);
        }
    }

    public void debug(String message) {
        writeToFile(NilumLogLevel.DEBUG, message);
        if (debugEnabled.getAsBoolean()) {
            sink.log(NilumLogLevel.DEBUG, message);
        }
    }

    public void info(String message) {
        log(NilumLogLevel.INFO, message);
    }

    public void warn(String message) {
        log(NilumLogLevel.WARN, message);
    }

    public void error(String message) {
        log(NilumLogLevel.ERROR, message);
    }

    public void error(String message, Throwable cause) {
        log(NilumLogLevel.ERROR, message + ": " + cause);
    }

    private void log(NilumLogLevel level, String message) {
        writeToFile(level, message);
        sink.log(level, message);
    }

    private void writeToFile(NilumLogLevel level, String message) {
        if (logFile == null) {
            return;
        }
        String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] [" + level + "] "
                + NilumConsoleFormat.plain(level, message) + System.lineSeparator();
        try {
            Files.writeString(logFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Can't log this failure without risking recursion into the same broken file; swallow it.
        }
    }
}
