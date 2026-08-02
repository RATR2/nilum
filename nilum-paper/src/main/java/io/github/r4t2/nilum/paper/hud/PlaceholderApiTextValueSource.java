package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Resolves placeholderapi(key) to a raw string for server_connector expressions by asking
 * PlaceholderAPI to expand %key%. Only ever constructed after confirming PlaceholderAPI is
 * installed (see HudTextService).
 */
public final class PlaceholderApiTextValueSource implements TextValueSource {

    private final Player player;

    public PlaceholderApiTextValueSource(Player player) {
        this.player = player;
    }

    @Override
    public String resolve(String function, String key) {
        // Only placeholderapi(...) is meaningful server-side; name(...)/head(...) need a real
        // client player object, which the server has no reason to fake here.
        return PlaceholderAPI.setPlaceholders(player, "%" + key + "%");
    }
}
