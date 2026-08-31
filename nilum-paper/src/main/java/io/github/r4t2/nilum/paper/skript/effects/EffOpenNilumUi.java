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

@Name("Open Nilum UI")
@Description("Opens a Nilum custom UI for a player.")
@Example("open nilum ui \"main_menu\" for player")
@Since("1.0")
public class EffOpenNilumUi extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffOpenNilumUi.class)
                .supplier(EffOpenNilumUi::new)
                .addPatterns("open [nilum] ui %string% for %player%")
                .build());
    }

    private Expression<String> uiId;
    private Expression<Player> player;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        uiId = (Expression<String>) exprs[0];
        player = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    public void execute(Event event) {
        String id = uiId.getSingle(event);
        Player target = player.getSingle(event);
        if (id == null || target == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return;
        }
        api.openCustomUi(target, id);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "open nilum ui " + uiId.toString(event, debug) + " for " + player.toString(event, debug);
    }
}
