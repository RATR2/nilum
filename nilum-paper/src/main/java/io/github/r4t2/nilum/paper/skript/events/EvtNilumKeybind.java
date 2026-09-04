package io.github.r4t2.nilum.paper.skript.events;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import io.github.r4t2.nilum.paper.event.NilumKeybindEvent;
import io.github.r4t2.nilum.paper.skript.types.NilumKeyState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Locale;

public class EvtNilumKeybind extends SkriptEvent {

    private static final NilumKeyState[] STATE_BY_MARK = {NilumKeyState.PRESS, NilumKeyState.RELEASE, NilumKeyState.BOTH};

    public static void register(SyntaxRegistry registry) {
        registry.register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtNilumKeybind.class, "Nilum Keybind")
                        .addEvent(NilumKeybindEvent.class)
                        .addPatterns("[nilum] keybind %integer% (0:press|1:release|2:both)")
                        .addDescription("Called when a player presses or releases one of Nilum's 4 general-purpose "
                                + "client keybinds. The number is 1-4, matching \"Nilum Keybind 1\"-\"Nilum Keybind 4\" "
                                + "in the client's Controls menu. Use \"player\" for who pressed it.")
                        .addExample("on nilum keybind 1 press:\n\tsend \"you pressed keybind 1\" to player")
                        .addExample("on nilum keybind 2 both:\n\tsend \"keybind 2 changed\" to player")
                        .addSince("1.0")
                        .supplier(EvtNilumKeybind::new)
                        .build());
    }

    private int slot;
    private NilumKeyState state;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        int typed = ((Literal<Number>) args[0]).getSingle().intValue();
        slot = typed - 1;
        state = STATE_BY_MARK[parseResult.mark];
        return slot >= 0 && slot < 4;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof NilumKeybindEvent keybind
                && keybind.getSlot() == slot
                && state.matches(keybind.isPressed());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on nilum keybind " + (slot + 1) + " " + state.name().toLowerCase(Locale.ROOT);
    }
}
