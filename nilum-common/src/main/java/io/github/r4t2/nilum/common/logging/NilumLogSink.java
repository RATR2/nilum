package io.github.r4t2.nilum.common.logging;

@FunctionalInterface
public interface NilumLogSink {
    void log(NilumLogLevel level, String rawMessage);
}
