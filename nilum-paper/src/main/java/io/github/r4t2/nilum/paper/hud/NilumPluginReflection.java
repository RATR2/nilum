package io.github.r4t2.nilum.paper.hud;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Resolves java(pluginName#memberName) by reflecting into another plugin's loaded instance. Only touches public members. */
final class NilumPluginReflection {

    private NilumPluginReflection() {
    }

    static Optional<Object> resolve(String key, Player player) {
        int hashIndex = key.indexOf('#');
        if (hashIndex < 0) {
            return Optional.empty();
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(key.substring(0, hashIndex));
        if (plugin == null || !plugin.isEnabled()) {
            return Optional.empty();
        }
        String memberName = key.substring(hashIndex + 1);

        return tryMethod(plugin, memberName, Player.class, player)
                .or(() -> tryMethod(plugin, memberName, OfflinePlayer.class, player))
                .or(() -> tryMethod(plugin, memberName, UUID.class, player.getUniqueId()))
                .or(() -> tryMethod(plugin, memberName))
                .or(() -> tryField(plugin, memberName));
    }

    static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1.0 : 0.0;
        }
        return 0.0;
    }

    static String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Optional<Object> tryMethod(Plugin plugin, String name, Class<?> paramType, Object arg) {
        try {
            Method method = plugin.getClass().getMethod(name, paramType);
            return Optional.ofNullable(method.invoke(plugin, arg));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return Optional.empty();
        }
    }

    private static Optional<Object> tryMethod(Plugin plugin, String name) {
        try {
            Method method = plugin.getClass().getMethod(name);
            return Optional.ofNullable(method.invoke(plugin));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return Optional.empty();
        }
    }

    private static Optional<Object> tryField(Plugin plugin, String name) {
        try {
            return Optional.ofNullable(plugin.getClass().getField(name).get(plugin));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }
}
