package io.github.r4t2.nilum.common.model;

import java.util.Map;

/**
 * A named animation track from a .bbmodel's "animations" array.
 *
 * @param loop              Blockbench's raw loop mode: "once", "hold", or "loop"
 * @param length            duration in seconds
 * @param animatorsByBoneUuid keyed by the animated BbOutlinerGroup's uuid (a "bone")
 */
public record BbAnimation(String uuid, String name, String loop, double length,
                           Map<String, BbAnimator> animatorsByBoneUuid) {
}
