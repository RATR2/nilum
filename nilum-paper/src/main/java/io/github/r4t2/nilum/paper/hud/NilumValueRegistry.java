package io.github.r4t2.nilum.paper.hud;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Lets other Paper plugins expose a live per-player value to server_connector expressions via
 * java(key), for plugins that don't have a PlaceholderAPI hook of their own.
 */
public final class NilumValueRegistry {

    private static final Map<String, Function<Player, Double>> numericValues = new ConcurrentHashMap<>();
    private static final Map<String, Function<Player, String>> textValues = new ConcurrentHashMap<>();

    private NilumValueRegistry() {
    }

    public static void registerNumeric(String key, Function<Player, Double> resolver) {
        numericValues.put(key, resolver);
    }

    public static void registerText(String key, Function<Player, String> resolver) {
        textValues.put(key, resolver);
    }

    static double resolveNumeric(String key, Player player) {
        Function<Player, Double> resolver = numericValues.get(key);
        return resolver == null ? 0 : resolver.apply(player);
    }

    static String resolveText(String key, Player player) {
        Function<Player, String> resolver = textValues.get(key);
        return resolver == null ? "" : resolver.apply(player);
    }
}
