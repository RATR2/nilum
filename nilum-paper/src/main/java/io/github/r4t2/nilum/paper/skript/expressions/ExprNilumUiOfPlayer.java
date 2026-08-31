package io.github.r4t2.nilum.paper.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import io.github.r4t2.nilum.api.NilumAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Nilum UI Of Player")
@Description("The id of the Nilum custom UI a player currently has open, or nothing if they don't have one open.")
@Example("broadcast \"%nilum ui of player%\"")
@Since("1.0")
public class ExprNilumUiOfPlayer extends SimplePropertyExpression<Player, String> {

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
                infoBuilder(ExprNilumUiOfPlayer.class, String.class, "[nilum] ui", "players", false)
                        .supplier(ExprNilumUiOfPlayer::new)
                        .build());
    }

    @Override
    public @Nullable String convert(Player player) {
        NilumAPI api = Bukkit.getServicesManager().load(NilumAPI.class);
        if (api == null) {
            return null;
        }
        return api.openCustomUiFor(player).orElse(null);
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "nilum ui";
    }
}
