package io.github.r4t2.nilum.paper.skript.events;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import io.github.r4t2.nilum.paper.event.NilumUiCloseEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EvtNilumUiClose extends SkriptEvent {

    public static void register(SyntaxRegistry registry) {
        registry.register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtNilumUiClose.class, "Nilum UI Close")
                        .addEvent(NilumUiCloseEvent.class)
                        .addPatterns("[nilum] ui close")
                        .addDescription("Called when a player closes a Nilum custom UI. Use \"player\" for who closed it and \"closed nilum ui\" for its id.")
                        .addExample("on ui close:\n\tbroadcast \"%player% closed %closed nilum ui%\"")
                        .addSince("1.0")
                        .supplier(EvtNilumUiClose::new)
                        .build());
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof NilumUiCloseEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on nilum ui close";
    }
}
