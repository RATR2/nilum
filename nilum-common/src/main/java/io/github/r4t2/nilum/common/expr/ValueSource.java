package io.github.r4t2.nilum.common.expr;

/** Resolves a numeric value-source call (number, boolean, placeholderapi, java, ...) to a number. Booleans are just 1.0/0.0. */
@FunctionalInterface
public interface ValueSource {
    double resolve(String function, String key);
}
