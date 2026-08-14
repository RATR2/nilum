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
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Place Nilum Model")
@Description("Places a loaded Nilum model as a standalone in-world display.")
@Example("place nilum model \"scanner\" at player's location")
@Since("1.0")
public class EffPlaceNilumModel extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffPlaceNilumModel.class)
                .supplier(EffPlaceNilumModel::new)
                .addPatterns("place nilum model %string% at %location%")
                .build());
    }

    private Expression<String> modelId;
    private Expression<Location> location;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        modelId = (Expression<String>) exprs[0];
        location = (Expression<Location>) exprs[1];
        return true;
    }

    @Override
    public void execute(Event event) {
        String id = modelId.getSingle(event);
        Location loc = location.getSingle(event);
        if (id == null || loc == null) {
            return;
        }
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api != null) {
            api.placeModel(loc, id);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "place nilum model " + modelId.toString(event, debug) + " at " + location.toString(event, debug);
    }
}
