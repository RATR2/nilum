package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.ValueSource;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Resolves placeholderapi(key) to a number for server_connector expressions, asking
 * PlaceholderAPI to expand %key% and parsing the result. Only ever constructed after
 * confirming PlaceholderAPI is installed (see HudTextService).
 */
public final class PlaceholderApiValueSource implements ValueSource {

    private final Player player;

    public PlaceholderApiValueSource(Player player) {
        this.player = player;
    }

    @Override
    public double resolve(String key) {
        String resolved = PlaceholderAPI.setPlaceholders(player, "%" + key + "%");
        try {
            return Double.parseDouble(resolved.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
