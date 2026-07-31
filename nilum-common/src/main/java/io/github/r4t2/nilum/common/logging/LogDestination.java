package io.github.r4t2.nilum.common.logging;

public enum LogDestination {
    CONSOLE,
    LOG,
    BOTH,
    DISABLED;

    public boolean toConsole() {
        return this == CONSOLE || this == BOTH;
    }

    public boolean toFile() {
        return this == LOG || this == BOTH;
    }
}
