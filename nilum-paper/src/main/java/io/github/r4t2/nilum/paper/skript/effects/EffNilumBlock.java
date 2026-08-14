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

@Name("Place/Remove Nilum Block")
@Description("Places or removes a real, rendered Nilum custom block.")
@Example("place nilum block \"crate\" at target block's location")
@Example("remove nilum block at target block's location")
@Since("1.0")
public class EffNilumBlock extends Effect {

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNilumBlock.class)
                .supplier(EffNilumBlock::new)
                .addPatterns("place nilum block %string% at %location%",
                        "remove nilum block at %location%")
                .build());
    }

    private boolean place;
    private @Nullable Expression<String> blockTypeId;
    private Expression<Location> location;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        place = matchedPattern == 0;
        if (place) {
            blockTypeId = (Expression<String>) exprs[0];
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
        if (place) {
            String id = blockTypeId.getSingle(event);
            if (id != null) {
                api.placeBlock(loc, id);
            }
        } else {
            api.removeBlock(loc);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return place
                ? "place nilum block " + blockTypeId.toString(event, debug) + " at " + location.toString(event, debug)
                : "remove nilum block at " + location.toString(event, debug);
    }
}
