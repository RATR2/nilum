package io.github.r4t2.nilum.paper.skript;

import ch.njol.skript.Skript;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.event.NilumUiCloseEvent;
import io.github.r4t2.nilum.paper.skript.effects.EffGiveNilumItem;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumBlock;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumBlockAnimation;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumEntityAnimation;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumShaderPack;
import io.github.r4t2.nilum.paper.skript.effects.EffOpenNilumUi;
import io.github.r4t2.nilum.paper.skript.effects.EffPlaceNilumModel;
import io.github.r4t2.nilum.paper.skript.effects.EffSetNilumHudFrame;
import io.github.r4t2.nilum.paper.skript.effects.EffSetNilumHudVisibility;
import io.github.r4t2.nilum.paper.skript.events.EvtNilumUiClose;
import io.github.r4t2.nilum.paper.skript.expressions.ExprClosedNilumUiId;
import io.github.r4t2.nilum.paper.skript.expressions.ExprNilumUiOfPlayer;
import org.bukkit.entity.Player;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.registration.SyntaxRegistry;

/** Registers Nilum's Skript effects via SkriptAddon/SyntaxRegistry. Only called after confirming Skript is installed. */
public final class NilumSkriptAddon {

    private NilumSkriptAddon() {
    }

    public static void register(NilumPlugin plugin) {
        SkriptAddon addon = Skript.instance().registerAddon(NilumPlugin.class, "Nilum");
        SyntaxRegistry syntaxRegistry = addon.syntaxRegistry();

        EffGiveNilumItem.register(syntaxRegistry);
        EffPlaceNilumModel.register(syntaxRegistry);
        EffNilumBlock.register(syntaxRegistry);
        EffNilumEntityAnimation.register(syntaxRegistry);
        EffNilumBlockAnimation.register(syntaxRegistry);
        EffNilumShaderPack.register(syntaxRegistry);
        EffOpenNilumUi.register(syntaxRegistry);
        EffSetNilumHudVisibility.register(syntaxRegistry);
        EffSetNilumHudFrame.register(syntaxRegistry);

        ExprNilumUiOfPlayer.register(syntaxRegistry);
        ExprClosedNilumUiId.register(syntaxRegistry);

        EvtNilumUiClose.register(syntaxRegistry);

        EventValueRegistry eventValues = addon.registry(EventValueRegistry.class);
        // Lets Skript's own generic "player" expression (and %player%/%uuid of player%
        // interpolation) resolve inside a Custom UI action's Skript effect line.
        eventValues.register(EventValue.simple(NilumUiActionEvent.class, Player.class, NilumUiActionEvent::player));
        // Same, for "player" inside "on ui close:".
        eventValues.register(EventValue.simple(NilumUiCloseEvent.class, Player.class, NilumUiCloseEvent::getPlayer));
    }
}
