package io.github.r4t2.nilum.paper.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.TriggerItem;
import org.bukkit.entity.Player;

/** Parses and runs one Skript effect line, for Custom UI action: config fields. */
public final class NilumSkriptEffectRunner {

    private NilumSkriptEffectRunner() {
    }

    /**
     * @param effectLine a single Skript effect, e.g. "delete {var::%player%}"
     * @param player the player the effect runs for; exposed to the effect as Skript's own "player" event value
     * @return false if the line failed to parse as a valid effect
     */
    public static boolean run(String effectLine, Player player) {
        Effect effect = Effect.parse(effectLine, null);
        if (effect == null) {
            return false;
        }
        TriggerItem.walk(effect, new NilumUiActionEvent(player));
        return true;
    }
}
