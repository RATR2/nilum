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
}
