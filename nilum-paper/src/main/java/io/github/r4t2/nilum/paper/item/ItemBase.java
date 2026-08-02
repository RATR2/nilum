package io.github.r4t2.nilum.paper.item;

import org.bukkit.Material;

/** What an ItemDefinition is actually built from. */
public sealed interface ItemBase {

    record Vanilla(Material material) implements ItemBase {
    }

    /** Wire-represented as baseMaterial plus a Nilum model tag, rendered as the real .bbmodel client-side. */
    record Model(String modelId, Material baseMaterial) implements ItemBase {
    }

    /** Wire-represented as baseMaterial plus a Nilum icon tag, rendered from the shared icon atlas client-side. */
    record Icon(String iconId, Material baseMaterial) implements ItemBase {
    }
}
