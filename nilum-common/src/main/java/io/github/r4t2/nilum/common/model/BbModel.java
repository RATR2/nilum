package io.github.r4t2.nilum.common.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record BbModel(
        String formatVersion,
        int resolutionWidth,
        int resolutionHeight,
        Map<String, BbElement> elementsByUuid,
        List<BbOutlinerNode> outliner,
        List<BbTexture> textures,
        Optional<String> collisionIntent,
        Map<String, BbDisplayTransform> display
) {

    public Optional<BbOutlinerGroup> findGroup(String name) {
        return findGroup(outliner, name);
    }

    private static Optional<BbOutlinerGroup> findGroup(List<BbOutlinerNode> nodes, String name) {
        for (BbOutlinerNode node : nodes) {
            if (node instanceof BbOutlinerGroup group) {
                if (group.name().equalsIgnoreCase(name)) {
                    return Optional.of(group);
                }
                Optional<BbOutlinerGroup> nested = findGroup(group.children(), name);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }

    public List<BbElement> resolveElements(BbOutlinerGroup group) {
        return collectElementUuids(group.children()).stream()
                .map(elementsByUuid::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static List<String> collectElementUuids(List<BbOutlinerNode> nodes) {
        List<String> uuids = new java.util.ArrayList<>();
        for (BbOutlinerNode node : nodes) {
            switch (node) {
                case BbOutlinerElementRef ref -> uuids.add(ref.elementUuid());
                case BbOutlinerGroup group -> uuids.addAll(collectElementUuids(group.children()));
            }
        }
        return uuids;
    }
}
