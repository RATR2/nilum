package io.github.r4t2.nilum.common.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public final class NilumLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NilumLogSink sink;
    private final Path logFile;
    private final Function<NilumLogLevel, LogDestination> destinationOf;

    public NilumLogger(NilumLogSink sink, Path logFile, Function<NilumLogLevel, LogDestination> destinationOf, int maxLogFiles) {
        this.sink = sink;
        this.logFile = logFile;
        this.destinationOf = destinationOf;
        if (logFile != null) {
            createParentDirectories(logFile);
            LogFileRotator.rotateOnStartup(logFile, maxLogFiles);
        }
    }

    private static void createParentDirectories(Path logFile) {
        Path parent = logFile.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            // Nothing more useful to do; writeToFile() below will just keep failing silently too.
        }
    }

    public void debug(String message) {
        route(NilumLogLevel.DEBUG, message);
    }

    /** Always shown in the console and written to the log file; not configurable. */
    public void info(String message) {
        writeToFile(NilumLogLevel.INFO, message);
        sink.log(NilumLogLevel.INFO, message);
    }

    public void warn(String message) {
        route(NilumLogLevel.WARN, message);
    }

    public void warn(String message, Throwable cause) {
        warn(message + ": " + causeChain(cause));
    }

    public void error(String message) {
        route(NilumLogLevel.ERROR, message);
    }

    public void error(String message, Throwable cause) {
        error(message + ": " + causeChain(cause));
    }

    public void moderation(String message) {
        route(NilumLogLevel.MODERATION, message);
    }

    /** Walks the full cause chain; Throwable#toString() alone drops everything past the outermost wrapper. */
    private static String causeChain(Throwable cause) {
        StringBuilder chain = new StringBuilder(String.valueOf(cause));
        Throwable next = cause.getCause();
        while (next != null && next != cause) {
            chain.append(" | caused by: ").append(next);
            cause = next;
            next = cause.getCause();
        }
        return chain.toString();
    }

    private void route(NilumLogLevel level, String message) {
        LogDestination destination = destinationOf.apply(level);
        if (destination.toFile()) {
            writeToFile(level, message);
        }
        if (destination.toConsole()) {
            sink.log(level, message);
        }
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
