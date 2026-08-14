package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Resolves server_connector text calls: placeholderapi(key) via PlaceholderAPI, java(key) via
 * NilumPluginReflection (key has '#') or NilumValueRegistry otherwise.
 */
public final class PlaceholderApiTextValueSource implements TextValueSource {

    private final Player player;

    public PlaceholderApiTextValueSource(Player player) {
        this.player = player;
    }

    @Override
    public String resolve(String function, String key) {
        if (function.equals("java")) {
            return key.indexOf('#') >= 0
                    ? NilumPluginReflection.resolve(key, player).map(NilumPluginReflection::toText).orElse("")
                    : NilumValueRegistry.resolveText(key, player);
        }
        return PlaceholderAPI.setPlaceholders(player, "%" + key + "%");
    }
}
