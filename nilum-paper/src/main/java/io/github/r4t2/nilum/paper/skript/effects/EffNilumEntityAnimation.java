package io.github.r4t2.nilum.paper.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import io.github.r4t2.nilum.paper.api.NilumAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Play/Stop Nilum Entity Animation")
@Description("Plays or stops a named animation on a player skeleton or placed model entity.")
@Example("play nilum animation \"reload\" on player")
@Example("stop nilum animation on player")
@Since("1.0")
public class EffNilumEntityAnimation extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNilumEntityAnimation.class)
                .supplier(EffNilumEntityAnimation::new)
                .addPatterns("play nilum animation %string% on %entity%",
                        "stop nilum animation on %entity%")
                .build());
    }

    private boolean play;
    private @Nullable Expression<String> animationName;
    private Expression<Entity> target;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        play = matchedPattern == 0;
        if (play) {
            animationName = (Expression<String>) exprs[0];
            target = (Expression<Entity>) exprs[1];
        } else {
            target = (Expression<Entity>) exprs[0];
        }
        return true;
    }

    @Override
    public void execute(Event event) {
        Entity entity = target.getSingle(event);
        if (entity == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        if (play) {
            String name = animationName.getSingle(event);
            if (name != null) {
                api.playEntityAnimation(entity.getUniqueId(), name);
            }
        } else {
            api.stopEntityAnimation(entity.getUniqueId());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return play
                ? "play nilum animation " + animationName.toString(event, debug) + " on " + target.toString(event, debug)
                : "stop nilum animation on " + target.toString(event, debug);
    }
}
