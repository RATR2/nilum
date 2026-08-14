package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import org.bukkit.entity.Player;

/** Resolves java(key): a '#' key reflects into another plugin, everything else goes through NilumValueRegistry's opt-in registration. */
public final class NilumJavaTextValueSource implements TextValueSource {

    private final Player player;

    public NilumJavaTextValueSource(Player player) {
        this.player = player;
    }

    @Override
    public String resolve(String function, String key) {
        if (!function.equals("java")) {
            return "";
        }
        return key.indexOf('#') >= 0
                ? NilumPluginReflection.resolve(key, player).map(NilumPluginReflection::toText).orElse("")
                : NilumValueRegistry.resolveText(key, player);
    }
}
