package io.github.r4t2.nilum.common.expr;

/** Resolves a text-producing value-source call (e.g. name("client")) to a display string, for render_text HUD elements. */
@FunctionalInterface
public interface TextValueSource {
    String resolve(String function, String key);
}
