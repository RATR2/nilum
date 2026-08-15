package io.github.r4t2.nilum.neoforge.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import net.minecraft.client.player.LocalPlayer;

/** Resolves name(...)/head(...) for render_text HUD elements; "client" is the only key so far. */
public final class NilumHudTextValueSource implements TextValueSource {

    private final LocalPlayer player;
    private final NilumLogger logger;

    public NilumHudTextValueSource(LocalPlayer player, NilumLogger logger) {
        this.player = player;
        this.logger = logger;
    }

    @Override
    public String resolve(String function, String key) {
        return switch (function) {
            case "name" -> resolveName(key);
            case "head" -> resolveHead(key);
            default -> unknown(function, key);
        };
    }

    private String resolveName(String key) {
        if (key.equals("client")) {
            return player.getGameProfile().name();
        }
        return unknown("name", key);
    }

    private String resolveHead(String key) {
        if (key.equals("client")) {
            // By UUID, not username; avoids any ambiguity with HudHeadText's own parsing.
            return "<head:" + player.getGameProfile().id() + ">";
        }
        return unknown("head", key);
    }

    private String unknown(String function, String key) {
        logger.warn("Unknown HUD text value source '" + function + "(\"" + key + "\")', resolving to an empty string.");
        return "";
    }
}
