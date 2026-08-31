package io.github.r4t2.nilum.paper.skript;

import ch.njol.skript.variables.Variables;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/** Reads and writes Skript's own global variables, so Nilum's config-driven systems can share state with Skript scripts. */
public final class NilumSkriptVariables {

    private NilumSkriptVariables() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Skript");
    }

    @Nullable
    public static Object get(String name) {
        return Variables.getVariable(name, null, false);
    }

    public static void set(String name, @Nullable Object value) {
        Variables.setVariable(name, value, null, false);
    }

    public static void delete(String name) {
        Variables.deleteVariable(name, null, false);
    }
}
