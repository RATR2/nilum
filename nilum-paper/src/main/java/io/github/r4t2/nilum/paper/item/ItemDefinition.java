package io.github.r4t2.nilum.paper.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A named, reusable item template: the canonical way any Nilum-aware system (block drops,
 * commands, other plugins) produces "the fancy version" of an item.
 */
public record ItemDefinition(
        String id,
        ItemBase base,
        Optional<String> displayName,
        List<String> lore,
        Map<String, Integer> enchantments,
        Optional<GlintDefinition> glint,
        List<String> hideGroups
) {
}
