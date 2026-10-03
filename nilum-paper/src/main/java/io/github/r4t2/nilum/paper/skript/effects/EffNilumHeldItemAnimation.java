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
@Description("Plays or stops a named animation on the Nilum item a player is holding in their main or off hand, optionally forcing a loop mode instead of the animation's own authored one.")
@Example("play nilum animation \"scan\" on player's main hand")
@Example("play nilum animation \"scan\" on player's main hand with loop mode hold")
@Example("stop nilum animation on player's main hand")
@Since("1.0")
public class EffNilumHeldItemAnimation extends Effect {

    // Hand uses bit 0 (0/1); loop mode uses bits 1-2 (0/2/4) so the two groups' marks never collide when summed.
    private static final int HAND_MASK = 0b1;
    private static final int LOOP_MODE_MASK = 0b110;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNilumHeldItemAnimation.class)
                .supplier(EffNilumHeldItemAnimation::new)
                .addPatterns("play nilum animation %string% on %player%'[s] (0:main|1:off) hand",
                        "play nilum animation %string% on %player%'[s] (0:main|1:off) hand with loop mode (0:once|2:hold|4:loop)",
                        "stop nilum animation on %player%'[s] (0:main|1:off) hand")
                .build());
    }

    private boolean play;
    private boolean mainHand;
    private @Nullable String loopModeOverride;
    private @Nullable Expression<String> animationName;
    private Expression<Player> target;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        play = matchedPattern != 2;
        mainHand = (parseResult.mark & HAND_MASK) == 0;
        if (play) {
            animationName = (Expression<String>) exprs[0];
            target = (Expression<Player>) exprs[1];
            if (matchedPattern == 1) {
                loopModeOverride = switch (parseResult.mark & LOOP_MODE_MASK) {
                    case 2 -> "hold";
                    case 4 -> "loop";
                    default -> "once";
                };
            }
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
                if (loopModeOverride != null) {
                    api.playHeldItemAnimation(player, mainHand, name, loopModeOverride);
                } else {
                    api.playHeldItemAnimation(player, mainHand, name);
                }
            }
        } else {
            api.stopHeldItemAnimation(player, mainHand);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String hand = mainHand ? "main" : "off";
        if (!play) {
            return "stop nilum animation on " + target.toString(event, debug) + "'s " + hand + " hand";
        }
        String base = "play nilum animation " + animationName.toString(event, debug) + " on " + target.toString(event, debug) + "'s " + hand + " hand";
        return loopModeOverride != null ? base + " with loop mode " + loopModeOverride : base;
    }
}
