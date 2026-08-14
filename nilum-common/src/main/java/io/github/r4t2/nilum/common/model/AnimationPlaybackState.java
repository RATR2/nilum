package io.github.r4t2.nilum.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One in-world model instance's current animation, with a short blend from the pose showing at
 * trigger time. Before any trigger arrives, an instance auto-loops the model's first animation.
 */
public final class AnimationPlaybackState {

    private static final long BLEND_DURATION_MILLIS = 200;

    private boolean triggered = false;
    private String animationName;
    private long startTimeMillis;
    private Map<String, BbMatrix4> blendFromPose;
    private long blendStartMillis;

    public synchronized void play(BbModel model, String animationName, long startTimeMillis, long nowMillis) {
        blendFromPose = pose(model, nowMillis);
        blendStartMillis = nowMillis;
        this.animationName = animationName;
        this.startTimeMillis = startTimeMillis;
        this.triggered = true;
    }

    public synchronized void stop(BbModel model, long nowMillis) {
        blendFromPose = pose(model, nowMillis);
        blendStartMillis = nowMillis;
        this.animationName = null;
        this.triggered = true;
    }

    public synchronized Map<String, BbMatrix4> pose(BbModel model, long nowMillis) {
        Map<String, BbMatrix4> target = triggered ? resolveTriggeredPose(model, nowMillis) : BbBonePose.computeAutoLoop(model);

        if (blendFromPose == null) {
            return target;
        }
        long blendElapsed = nowMillis - blendStartMillis;
        if (blendElapsed >= BLEND_DURATION_MILLIS) {
            blendFromPose = null;
            return target;
        }
        float t = Math.max(0f, blendElapsed / (float) BLEND_DURATION_MILLIS);
        return lerpPoses(blendFromPose, target, t);
    }

    private Map<String, BbMatrix4> resolveTriggeredPose(BbModel model, long nowMillis) {
        if (animationName == null) {
            return BbBonePose.compute(model, null, 0);
        }
        BbAnimation animation = model.findAnimation(animationName).orElse(null);
        if (animation == null || animation.length() <= 0) {
            return BbBonePose.compute(model, null, 0);
        }

        double elapsed = Math.max(0, (nowMillis - startTimeMillis) / 1000.0);
        return switch (animation.loop()) {
            case "hold" -> BbBonePose.compute(model, animation, Math.min(elapsed, animation.length()));
            case "once" -> elapsed >= animation.length()
                    ? BbBonePose.compute(model, null, 0)
                    : BbBonePose.compute(model, animation, elapsed);
            default -> BbBonePose.compute(model, animation, elapsed % animation.length());
        };
    }

    private static Map<String, BbMatrix4> lerpPoses(Map<String, BbMatrix4> from, Map<String, BbMatrix4> to, float t) {
        Set<String> bones = new HashSet<>(from.keySet());
        bones.addAll(to.keySet());

        Map<String, BbMatrix4> result = new HashMap<>();
        for (String bone : bones) {
            BbMatrix4 a = from.getOrDefault(bone, BbMatrix4.identity());
            BbMatrix4 b = to.getOrDefault(bone, BbMatrix4.identity());
            result.put(bone, a.lerp(b, t));
        }
        return result;
    }
}
