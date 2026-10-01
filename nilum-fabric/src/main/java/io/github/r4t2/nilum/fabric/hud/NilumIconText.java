package io.github.r4t2.nilum.fabric.hud;

import io.github.r4t2.nilum.fabric.NilumFabricClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces &lt;icon:id&gt; tags with the streamed font-provider icon's auto-assigned codepoint, same convention as HudHeadText. */
public final class NilumIconText {

    private static final Pattern ICON_TAG = Pattern.compile("<icon:([^>]+)>");

    private NilumIconText() {
    }

    public static String replace(String raw) {
        if (!raw.contains("<icon:")) {
            return raw;
        }
        Matcher matcher = ICON_TAG.matcher(raw);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(raw, lastEnd, matcher.start());
            Integer codepoint = NilumFabricClient.FONT_ICON_STORE.codepointFor(matcher.group(1)).orElse(null);
            result.append(codepoint != null ? Character.toChars(codepoint) : matcher.group().toCharArray());
            lastEnd = matcher.end();
        }
        result.append(raw, lastEnd, raw.length());
        return result.toString();
    }
}
