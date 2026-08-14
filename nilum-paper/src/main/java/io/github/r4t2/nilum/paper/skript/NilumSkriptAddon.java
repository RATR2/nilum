package io.github.r4t2.nilum.paper.skript;

import ch.njol.skript.Skript;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.skript.effects.EffGiveNilumItem;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumBlock;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumBlockAnimation;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumEntityAnimation;
import io.github.r4t2.nilum.paper.skript.effects.EffNilumShaderPack;
import io.github.r4t2.nilum.paper.skript.effects.EffPlaceNilumModel;
import org.skriptlang.skript.addon.SkriptAddon;
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
    }
}
