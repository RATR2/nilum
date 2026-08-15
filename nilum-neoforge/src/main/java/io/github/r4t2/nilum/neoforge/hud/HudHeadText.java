package io.github.r4t2.nilum.neoforge.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses <head:identifier[:hat]> tags into an inline player-head glyph, same syntax as Paper's MiniMessage head tag. */
public final class HudHeadText {

    private static final Pattern HEAD_TAG = Pattern.compile("<head:([^:>]+)(?::([^>]+))?>");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abcdABCD][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    private HudHeadText() {
    }

    public static boolean containsHeadTag(String text) {
        return text.contains("<head:");
    }

    public static Component parse(String raw) {
        Matcher matcher = HEAD_TAG.matcher(raw);
        MutableComponent result = Component.empty();
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.append(raw.substring(lastEnd, matcher.start()));
            }
            result.append(headComponent(matcher.group(1), matcher.group(2)));
            lastEnd = matcher.end();
        }
        if (lastEnd < raw.length()) {
            result.append(raw.substring(lastEnd));
        }
        return result;
    }

    private static Component headComponent(String identifier, String hatArgument) {
        boolean hat = hatArgument == null || Boolean.parseBoolean(hatArgument);
        ResolvableProfile profile = UUID_PATTERN.matcher(identifier).matches()
                ? ResolvableProfile.createUnresolved(UUID.fromString(identifier))
                : ResolvableProfile.createUnresolved(identifier);
        return Component.object(new PlayerSprite(profile, hat));
    }
}
