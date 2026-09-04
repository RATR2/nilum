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

@Name("Play/Stop Nilum Held Item Animation")
@Description("Plays or stops a named animation on the Nilum item a player is holding in their main or off hand.")
@Example("play nilum animation \"scan\" on player's main hand")
@Example("stop nilum animation on player's main hand")
@Since("1.0")
public class EffNilumHeldItemAnimation extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNilumHeldItemAnimation.class)
                .supplier(EffNilumHeldItemAnimation::new)
                .addPatterns("play nilum animation %string% on %player%'[s] (0:main|1:off) hand",
                        "stop nilum animation on %player%'[s] (0:main|1:off) hand")
                .build());
    }

    private boolean play;
    private boolean mainHand;
    private @Nullable Expression<String> animationName;
    private Expression<Player> target;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        play = matchedPattern == 0;
        mainHand = parseResult.mark == 0;
        if (play) {
            animationName = (Expression<String>) exprs[0];
            target = (Expression<Player>) exprs[1];
        } else {
            target = (Expression<Player>) exprs[0];
        }
        return true;
    }

    @Override
    public void execute(Event event) {
        Player player = target.getSingle(event);
        if (player == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        if (play) {
            String name = animationName.getSingle(event);
            if (name != null) {
                api.playHeldItemAnimation(player, mainHand, name);
            }
        } else {
            api.stopHeldItemAnimation(player, mainHand);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String hand = mainHand ? "main" : "off";
        return play
                ? "play nilum animation " + animationName.toString(event, debug) + " on " + target.toString(event, debug) + "'s " + hand + " hand"
                : "stop nilum animation on " + target.toString(event, debug) + "'s " + hand + " hand";
    }
}
