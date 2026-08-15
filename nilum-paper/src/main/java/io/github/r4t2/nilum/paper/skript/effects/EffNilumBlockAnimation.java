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
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Play/Stop Nilum Block Animation")
@Description("Plays or stops a named animation on a Nilum block.")
@Example("play nilum animation \"open\" on block at target block's location")
@Example("stop nilum animation on block at target block's location")
@Since("1.0")
public class EffNilumBlockAnimation extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNilumBlockAnimation.class)
                .supplier(EffNilumBlockAnimation::new)
                .addPatterns("play nilum animation %string% on [the] block at %location%",
                        "stop nilum animation on [the] block at %location%")
                .build());
    }

    private boolean play;
    private @Nullable Expression<String> animationName;
    private Expression<Location> location;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        play = matchedPattern == 0;
        if (play) {
            animationName = (Expression<String>) exprs[0];
            location = (Expression<Location>) exprs[1];
        } else {
            location = (Expression<Location>) exprs[0];
        }
        return true;
    }

    @Override
    public void execute(Event event) {
        Location loc = location.getSingle(event);
        if (loc == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        if (play) {
            String name = animationName.getSingle(event);
            if (name != null) {
                api.playBlockAnimation(loc, name);
            }
        } else {
            api.stopBlockAnimation(loc);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return play
                ? "play nilum animation " + animationName.toString(event, debug) + " on block at " + location.toString(event, debug)
                : "stop nilum animation on block at " + location.toString(event, debug);
    }
}
