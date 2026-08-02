package io.github.r4t2.nilum.common.expr;

/**
 * Resolves a text-producing value-source call (e.g. name("client"), head("client")) to a
 * display string, for render_text HUD elements. Distinct from ValueSource, which only produces
 * numbers. function is the calling function's name; implementations switch on it.
 */
@FunctionalInterface
public interface TextValueSource {
    String resolve(String function, String key);
}
