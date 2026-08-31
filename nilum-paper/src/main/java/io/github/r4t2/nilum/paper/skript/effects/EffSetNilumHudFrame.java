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

@Name("Set Nilum HUD Frame")
@Description("Sets a HUD atlas element's frame (atlasId:elementId) for a player.")
@Example("set nilum hud frame \"hud_demo:health_bar\" for player to 3")
@Since("1.0")
public class EffSetNilumHudFrame extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffSetNilumHudFrame.class)
                .supplier(EffSetNilumHudFrame::new)
                .addPatterns("set [nilum] hud frame %string% for %player% to %integer%")
                .build());
    }

    private Expression<String> target;
    private Expression<Player> player;
    private Expression<Long> frame;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        target = (Expression<String>) exprs[0];
        player = (Expression<Player>) exprs[1];
        frame = (Expression<Long>) exprs[2];
        return true;
    }

    @Override
    public void execute(Event event) {
        String targetValue = target.getSingle(event);
        Player targetPlayer = player.getSingle(event);
        Long frameValue = frame.getSingle(event);
        if (targetValue == null || targetPlayer == null || frameValue == null) {
            return;
        }

        String[] parts = targetValue.split(":", 2);
        if (parts.length != 2) {
            return;
        }

        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        api.setHudFrame(targetPlayer, parts[0], parts[1], frameValue.intValue());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set nilum hud frame " + target.toString(event, debug) + " for " + player.toString(event, debug)
                + " to " + frame.toString(event, debug);
    }
}
