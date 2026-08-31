package io.github.r4t2.nilum.common.model;

import java.util.Map;
import java.util.Optional;

/** Vanilla's generated.json flat-item display defaults, shared between icon and full-model display resolution. */
public final class VanillaGeneratedDisplay {

    public static final Map<String, BbDisplayTransform> DEFAULTS = Map.of(
            "ground", new BbDisplayTransform(Optional.empty(), Optional.of(new BbVector3(0, 2, 0)), Optional.of(new BbVector3(0.5, 0.5, 0.5))),
            "head", new BbDisplayTransform(Optional.of(new BbVector3(0, 180, 0)), Optional.of(new BbVector3(0, 13, 7)), Optional.of(new BbVector3(1, 1, 1))),
            "thirdperson_righthand", new BbDisplayTransform(Optional.empty(), Optional.of(new BbVector3(0, 3, 1)), Optional.of(new BbVector3(0.55, 0.55, 0.55))),
            "firstperson_righthand", new BbDisplayTransform(Optional.of(new BbVector3(0, -90, 25)), Optional.of(new BbVector3(1.13, 3.2, 1.13)), Optional.of(new BbVector3(0.68, 0.68, 0.68))),
            "fixed", new BbDisplayTransform(Optional.of(new BbVector3(0, 180, 0)), Optional.empty(), Optional.of(new BbVector3(1, 1, 1)))
    );

    private static final BbDisplayTransform EMPTY = new BbDisplayTransform(Optional.empty(), Optional.empty(), Optional.empty());

    private VanillaGeneratedDisplay() {
    }

    public static BbDisplayTransform forContext(String context) {
        return DEFAULTS.getOrDefault(context, EMPTY);
    }
}
