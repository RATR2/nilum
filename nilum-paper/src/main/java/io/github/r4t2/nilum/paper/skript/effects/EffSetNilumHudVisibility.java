package io.github.r4t2.nilum.paper.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import io.github.r4t2.nilum.api.NilumAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Set Nilum HUD Visibility")
@Description("Shows or hides a Nilum HUD atlas, or one element within it (atlasId:elementId), for a player.")
@Example("set nilum hud \"hud_demo\" for player to false")
@Example("set nilum hud \"hud_demo:health_bar\" for player to true")
@Since("1.0")
public class EffSetNilumHudVisibility extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffSetNilumHudVisibility.class)
                .supplier(EffSetNilumHudVisibility::new)
                .addPatterns("set [nilum] hud %string% for %player% to %boolean%")
                .build());
    }

    private Expression<String> target;
    private Expression<Player> player;
    private Expression<Boolean> visible;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        target = (Expression<String>) exprs[0];
        player = (Expression<Player>) exprs[1];
        visible = (Expression<Boolean>) exprs[2];
        return true;
    }

    @Override
    public void execute(Event event) {
        String targetValue = target.getSingle(event);
        Player targetPlayer = player.getSingle(event);
        Boolean visibleValue = visible.getSingle(event);
        if (targetValue == null || targetPlayer == null || visibleValue == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }

        String[] parts = targetValue.split(":", 2);
        if (parts.length == 2) {
            api.setHudElementVisible(targetPlayer, parts[0], parts[1], visibleValue);
        } else {
            api.setHudAtlasVisible(targetPlayer, targetValue, visibleValue);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set nilum hud " + target.toString(event, debug) + " for " + player.toString(event, debug)
                + " to " + visible.toString(event, debug);
    }
}
