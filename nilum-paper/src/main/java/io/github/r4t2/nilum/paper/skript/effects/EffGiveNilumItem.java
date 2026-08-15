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

@Name("Give Nilum Item")
@Description("Gives a player an item defined in Nilum's items folder (items/<id>.yml).")
@Example("give player the nilum item \"ray_gun\"")
@Since("1.0")
public class EffGiveNilumItem extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffGiveNilumItem.class)
                .supplier(EffGiveNilumItem::new)
                .addPatterns("give %player% [the] nilum item %string%")
                .build());
    }

    private Expression<Player> player;
    private Expression<String> itemId;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        player = (Expression<Player>) exprs[0];
        itemId = (Expression<String>) exprs[1];
        return true;
    }

    @Override
    public void execute(Event event) {
        Player target = player.getSingle(event);
        String id = itemId.getSingle(event);
        if (target == null || id == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        api.createDefinedItem(id).ifPresent(item -> target.getInventory().addItem(item));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "give " + player.toString(event, debug) + " the nilum item " + itemId.toString(event, debug);
    }
}
