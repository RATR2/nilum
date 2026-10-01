package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.paper.skript.NilumSkriptVariables;
import org.bukkit.entity.Player;

/** Coerces a Skript global variable's value for skriptvar(...) in Nilum's expression language. */
public final class SkriptVariableValueSource {

    private SkriptVariableValueSource() {
    }

    /** "{player}" anywhere in name is replaced with the evaluating player's UUID, for per-player list variables. */
    public static double resolveNumeric(String name, Player player) {
        Object value = NilumSkriptVariables.get(substitutePlayer(name, player));
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

    public static String resolveText(String name, Player player) {
        Object value = NilumSkriptVariables.get(substitutePlayer(name, player));
        return value == null ? "" : String.valueOf(value);
    }

    private static String substitutePlayer(String name, Player player) {
        return name.replace("{player}", player.getUniqueId().toString());
    }
}
