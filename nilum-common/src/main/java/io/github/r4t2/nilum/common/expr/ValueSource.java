package io.github.r4t2.nilum.common.expr;

/**
 * Resolves a number("key")/boolean("key") value-source call to a number. Booleans are just
 * 1.0/0.0 under the hood; the two names exist purely for author readability.
 */
@FunctionalInterface
public interface ValueSource {
    double resolve(String key);
}
