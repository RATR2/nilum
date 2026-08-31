package io.github.r4t2.nilum.common.model;

import java.util.Map;

public record BbElement(
        String uuid,
        String name,
        BbVector3 from,
        BbVector3 to,
        BbVector3 origin,
        BbVector3 rotation,
        Map<String, BbFace> faces
) {

    /** Bounding size of from/to (always positive), in the element's own unrotated local space. */
    public BbVector3 size() {
        return new BbVector3(Math.abs(to.x() - from.x()), Math.abs(to.y() - from.y()), Math.abs(to.z() - from.z()));
    }
}
