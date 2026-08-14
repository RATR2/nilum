package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.ValueSource;
import org.bukkit.entity.Player;

/** Resolves java(key): a '#' key reflects into another plugin, everything else goes through NilumValueRegistry's opt-in registration. */
public final class NilumJavaValueSource implements ValueSource {

    private final Player player;

    public NilumJavaValueSource(Player player) {
        this.player = player;
    }

    @Override
    public double resolve(String function, String key) {
        if (!function.equals("java")) {
            return 0;
        }
        return key.indexOf('#') >= 0
                ? NilumPluginReflection.resolve(key, player).map(NilumPluginReflection::toDouble).orElse(0.0)
                : NilumValueRegistry.resolveNumeric(key, player);
    }
}
