package io.github.r4t2.nilum.common.model;

import java.util.List;

public record BbOutlinerGroup(
        String uuid,
        String name,
        BbVector3 origin,
        BbVector3 rotation,
        List<BbOutlinerNode> children
) implements BbOutlinerNode {
}
