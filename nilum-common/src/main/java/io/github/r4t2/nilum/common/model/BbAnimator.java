package io.github.r4t2.nilum.common.model;

import java.util.List;

/** One bone's (or effect track's) keyframes within a BbAnimation. */
public record BbAnimator(String name, String type, List<BbKeyframe> keyframes) {
}
