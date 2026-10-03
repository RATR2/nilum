package io.github.r4t2.nilum.common.util;

/** Either a literal pixel count or a fraction of the current screen dimension, resolved fresh every frame. */
public sealed interface ScreenValue {

    int resolve(int screenDimension);

    record Pixels(int value) implements ScreenValue {
        @Override
        public int resolve(int screenDimension) {
            return value;
        }
    }

    record Percent(float fraction) implements ScreenValue {
        @Override
        public int resolve(int screenDimension) {
            return Math.round(fraction * screenDimension);
        }
    }

    static ScreenValue parse(String raw) {
        String trimmed = raw.trim();
        if (trimmed.endsWith("%")) {
            return new Percent(Float.parseFloat(trimmed.substring(0, trimmed.length() - 1)) / 100.0F);
        }
        return new Pixels(Integer.parseInt(trimmed));
    }
}
