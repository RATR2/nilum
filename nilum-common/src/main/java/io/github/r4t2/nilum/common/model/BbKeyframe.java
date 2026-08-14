package io.github.r4t2.nilum.common.model;

/** One keyframe on a BbAnimator's track. Only plain numbers are supported; anything else (e.g. molang expressions) parses as 0. */
public record BbKeyframe(String channel, double time, String interpolation, BbVector3 value) {
}
