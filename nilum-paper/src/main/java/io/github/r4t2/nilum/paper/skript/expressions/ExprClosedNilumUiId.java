package io.github.r4t2.nilum.paper.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import io.github.r4t2.nilum.paper.event.NilumUiCloseEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Closed Nilum UI")
@Description("The id of the Nilum custom UI that was just closed. Only usable in an \"on ui close\" event.")
@Example("on ui close:\n\tbroadcast \"%player% closed %closed nilum ui%\"")
@Events("ui close")
@Since("1.0")
public class ExprClosedNilumUiId extends SimpleExpression<String> {

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprClosedNilumUiId.class, String.class)
                .addPatterns("[the] closed [nilum] ui")
                .supplier(ExprClosedNilumUiId::new)
                .build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        return true;
    }

    @Override
    protected String @Nullable [] get(Event event) {
        if (!(event instanceof NilumUiCloseEvent closeEvent)) {
            return null;
        }
        return new String[]{closeEvent.getUiId()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the closed nilum ui";
    }
}
