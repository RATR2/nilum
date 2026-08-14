package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.ValueSource;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Resolves server_connector value-source calls: placeholderapi(key) via PlaceholderAPI, java(key)
 * via NilumPluginReflection (key has '#') or NilumValueRegistry otherwise.
 */
public final class PlaceholderApiValueSource implements ValueSource {

    private final Player player;

    public PlaceholderApiValueSource(Player player) {
        this.player = player;
    }

    @Override
    public double resolve(String function, String key) {
        if (function.equals("java")) {
            return key.indexOf('#') >= 0
                    ? NilumPluginReflection.resolve(key, player).map(NilumPluginReflection::toDouble).orElse(0.0)
                    : NilumValueRegistry.resolveNumeric(key, player);
        }
        String resolved = PlaceholderAPI.setPlaceholders(player, "%" + key + "%");
        try {
            return Double.parseDouble(resolved.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
