package io.github.r4t2.nilum.paper.logging;

import io.github.r4t2.nilum.common.logging.NilumConsoleFormat;
import io.github.r4t2.nilum.common.logging.NilumLogLevel;
import io.github.r4t2.nilum.common.logging.NilumLogSink;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;

/** Sends branded log lines directly to the console as an Adventure Component, bypassing java.util.logging's own prefix. */
public final class PaperConsoleSink implements NilumLogSink {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void log(NilumLogLevel level, String rawMessage) {
        String color = NilumConsoleFormat.messageColorHex(level);
        String template = "<#" + NilumConsoleFormat.PREFIX_COLOR_HEX + ">" + NilumConsoleFormat.PREFIX_TEXT
                + "</#" + NilumConsoleFormat.PREFIX_COLOR_HEX + ">"
                + "<#" + color + "><message></#" + color + ">";

        Component component = MINI_MESSAGE.deserialize(template, Placeholder.unparsed("message", rawMessage));
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
