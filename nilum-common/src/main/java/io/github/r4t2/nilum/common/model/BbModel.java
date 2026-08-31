package io.github.r4t2.nilum.common.model;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record BbModel(
        String formatVersion,
        int resolutionWidth,
        int resolutionHeight,
        Map<String, BbElement> elementsByUuid,
        List<BbOutlinerNode> outliner,
        List<BbTexture> textures,
        Optional<String> collisionIntent,
        Map<String, BbDisplayTransform> display,
        List<BbAnimation> animations
) {

    public Optional<BbAnimation> findAnimation(String name) {
        return animations.stream().filter(animation -> animation.name().equals(name)).findFirst();
    }

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

    private static final Set<String> HAND_MARKER_GROUP_NAMES = Set.of("right arm", "left arm");

    /** Elements under the hand-IK "Right arm"/"Left arm" marker groups, hidden from normal rendering (see ItemInHandRendererMixin). */
    public Set<String> handMarkerElementUuids() {
        Set<String> uuids = new HashSet<>();
        collectHandMarkerUuids(outliner, uuids);
        return uuids;
    }

    private static void collectHandMarkerUuids(List<BbOutlinerNode> nodes, Set<String> out) {
        for (BbOutlinerNode node : nodes) {
            if (node instanceof BbOutlinerGroup group) {
                if (HAND_MARKER_GROUP_NAMES.contains(group.name().toLowerCase(Locale.ROOT))) {
                    out.addAll(collectElementUuids(group.children()));
                } else {
                    collectHandMarkerUuids(group.children(), out);
                }
            }
        }
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
