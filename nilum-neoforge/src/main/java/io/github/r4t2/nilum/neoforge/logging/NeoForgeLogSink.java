package io.github.r4t2.nilum.neoforge.logging;

import io.github.r4t2.nilum.common.logging.NilumConsoleFormat;
import io.github.r4t2.nilum.common.logging.NilumLogLevel;
import io.github.r4t2.nilum.common.logging.NilumLogSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bridges Nilum's logger to SLF4J, which Minecraft and NeoForge/FML already use. */
public final class NeoForgeLogSink implements NilumLogSink {

    private static final Logger LOGGER = LoggerFactory.getLogger("nilum");

    @Override
    public void log(NilumLogLevel level, String rawMessage) {
        String formatted = NilumConsoleFormat.ansi(level, rawMessage);
        switch (level) {
            case DEBUG -> LOGGER.debug(formatted);
            case INFO, MODERATION -> LOGGER.info(formatted);
            case WARN -> LOGGER.warn(formatted);
            case ERROR -> LOGGER.error(formatted);
        }
    }
}
