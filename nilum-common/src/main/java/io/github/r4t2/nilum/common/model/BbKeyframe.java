package io.github.r4t2.nilum.common.model;

/**
 * One keyframe on a BbAnimator's track. value is the first of Blockbench's "data_points", the
 * only one linear/step/catmullrom interpolation uses. Only plain numbers are supported for now;
 * anything else (including molang-style expressions) parses as 0.
 */
public record BbKeyframe(String channel, double time, String interpolation, BbVector3 value) {
}
