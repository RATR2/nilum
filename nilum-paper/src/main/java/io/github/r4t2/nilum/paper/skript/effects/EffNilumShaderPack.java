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

@Name("Switch/Reset Nilum Shaderpack")
@Description("Switches a player's client to a Nilum shaderpack, or back to their previous state. Needs Iris on their client.")
@Example("switch nilum shaderpack of player to \"cave\"")
@Example("reset nilum shaderpack of player")
@Since("1.0")
public class EffNilumShaderPack extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNilumShaderPack.class)
                .supplier(EffNilumShaderPack::new)
                .addPatterns("switch nilum shaderpack of %player% to %string%",
                        "reset nilum shaderpack of %player%")
                .build());
    }

    private boolean activate;
    private Expression<Player> player;
    private @Nullable Expression<String> packId;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        activate = matchedPattern == 0;
        player = (Expression<Player>) exprs[0];
        if (activate) {
            packId = (Expression<String>) exprs[1];
        }
        return true;
    }

    @Override
    public void execute(Event event) {
        Player target = player.getSingle(event);
        if (target == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        if (activate) {
            String id = packId.getSingle(event);
            if (id != null) {
                api.activateShaderPack(target, id);
            }
        } else {
            api.deactivateShaderPack(target);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return activate
                ? "switch nilum shaderpack of " + player.toString(event, debug) + " to " + packId.toString(event, debug)
                : "reset nilum shaderpack of " + player.toString(event, debug);
    }
}
