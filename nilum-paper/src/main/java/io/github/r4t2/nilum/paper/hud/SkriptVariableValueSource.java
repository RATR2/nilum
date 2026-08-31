package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.paper.skript.NilumSkriptVariables;

/** Coerces a Skript global variable's value for skriptvar(...) in the HUD expression language. */
final class SkriptVariableValueSource {

    private SkriptVariableValueSource() {
    }

    static double resolveNumeric(String name) {
        Object value = NilumSkriptVariables.get(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    static String resolveText(String name) {
        Object value = NilumSkriptVariables.get(name);
        return value == null ? "" : String.valueOf(value);
    }
}
