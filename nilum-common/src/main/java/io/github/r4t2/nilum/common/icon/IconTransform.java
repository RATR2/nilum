package io.github.r4t2.nilum.common.icon;

import io.github.r4t2.nilum.common.model.BbVector3;

/** A fully resolved per-context display transform - no more "generated"/"blockbench" keywords, just concrete numbers. */
public record IconTransform(BbVector3 rotation, BbVector3 translation, BbVector3 scale) {

    public static final IconTransform IDENTITY = new IconTransform(BbVector3.ZERO, BbVector3.ZERO, new BbVector3(1, 1, 1));
}
