package io.github.r4t2.nilum.common.model;

import java.util.Comparator;
import java.util.List;

/**
 * Evaluates one BbAnimator channel (rotation/position/scale) at a point in time, per
 * Blockbench's interpolation modes. "step" holds the previous keyframe's value; "catmullrom"
 * is a 4-point Catmull-Rom spline; everything else, including bezier handle data (not yet
 * supported), falls back to linear interpolation between the surrounding keyframes.
 */
public final class BbAnimationEvaluator {

    private BbAnimationEvaluator() {
    }

    public static BbVector3 evaluate(BbAnimator animator, String channel, double time, BbVector3 defaultValue) {
        List<BbKeyframe> track = animator.keyframes().stream()
                .filter(keyframe -> keyframe.channel().equals(channel))
                .sorted(Comparator.comparingDouble(BbKeyframe::time))
                .toList();

        if (track.isEmpty()) {
            return defaultValue;
        }
        if (track.size() == 1 || time <= track.get(0).time()) {
            return track.get(0).value();
        }
        if (time >= track.get(track.size() - 1).time()) {
            return track.get(track.size() - 1).value();
        }

        int index = findSegmentStart(track, time);
        BbKeyframe k1 = track.get(index);
        BbKeyframe k2 = track.get(index + 1);
        double span = k2.time() - k1.time();
        double t = span <= 0 ? 0 : (time - k1.time()) / span;

        return switch (k2.interpolation()) {
            case "step" -> k1.value();
            case "catmullrom" -> {
                BbVector3 p0 = index > 0 ? track.get(index - 1).value() : k1.value();
                BbVector3 p3 = index + 2 < track.size() ? track.get(index + 2).value() : k2.value();
                yield catmullRom(p0, k1.value(), k2.value(), p3, t);
            }
            default -> lerp(k1.value(), k2.value(), t);
        };
    }

    private static int findSegmentStart(List<BbKeyframe> track, double time) {
        for (int i = 0; i < track.size() - 1; i++) {
            if (time <= track.get(i + 1).time()) {
                return i;
            }
        }
        return track.size() - 2;
    }

    private static BbVector3 lerp(BbVector3 a, BbVector3 b, double t) {
        return new BbVector3(
                a.x() + (b.x() - a.x()) * t,
                a.y() + (b.y() - a.y()) * t,
                a.z() + (b.z() - a.z()) * t);
    }

    private static BbVector3 catmullRom(BbVector3 p0, BbVector3 p1, BbVector3 p2, BbVector3 p3, double t) {
        return new BbVector3(
                catmullRomComponent(p0.x(), p1.x(), p2.x(), p3.x(), t),
                catmullRomComponent(p0.y(), p1.y(), p2.y(), p3.y(), t),
                catmullRomComponent(p0.z(), p1.z(), p2.z(), p3.z(), t));
    }

    private static double catmullRomComponent(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2 * p1)
                + (-p0 + p2) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}
