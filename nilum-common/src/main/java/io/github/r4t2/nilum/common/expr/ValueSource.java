package io.github.r4t2.nilum.common.expr;

/**
 * Resolves a {@code number("key")}/{@code boolean("key")} value-source call to a number.
 * Booleans are just 1.0/0.0 under the hood - there's no separate boolean type in this DSL, so
 * both functions resolve identically; the two names exist purely for author readability.
 */
@FunctionalInterface
public interface ValueSource {
    double resolve(String key);
}
