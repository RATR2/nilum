package io.github.r4t2.nilum.paper.block;

import org.bukkit.Material;

/**
 * How a Nilum block's real-world mechanics are decided. Either way the actual wire block is a
 * real vanilla Material; Paper can't put non-vanilla block state on the wire.
 */
public sealed interface BlockProxy {

    Material wireMaterial();

    /** Trusts material's own real mechanics (hardness, tool, drops, flammability, sound) as-is. */
    record Vanilla(Material material) implements BlockProxy {
        @Override
        public Material wireMaterial() {
            return material;
        }
    }

    /** Mechanics enforced manually; wireBlock must still be a real breakable material. Creative insta-break is always suppressed. */
    record Custom(Material wireBlock, double breakTimeSeconds, boolean explosionResistant) implements BlockProxy {
        @Override
        public Material wireMaterial() {
            return wireBlock;
        }
    }
}
