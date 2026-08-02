package io.github.r4t2.nilum.common.icon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every ItemDisplayContext an icon item needs a placement for, fully resolved server-side so
 * the client only ever deals with concrete numbers.
 */
public record IconDisplay(Map<String, IconTransform> byContext) {

    /** All 9 real (non-NONE) vanilla ItemDisplayContext serialized names. */
    public static final List<String> CONTEXTS = List.of(
            "thirdperson_lefthand", "thirdperson_righthand",
            "firstperson_lefthand", "firstperson_righthand",
            "head", "gui", "ground", "fixed", "on_shelf");

    public IconTransform transformFor(String context) {
        return byContext.getOrDefault(context, IconTransform.IDENTITY);
    }

    public static IconDisplay allIdentity() {
        Map<String, IconTransform> map = new HashMap<>();
        for (String context : CONTEXTS) {
            map.put(context, IconTransform.IDENTITY);
        }
        return new IconDisplay(map);
    }
}
