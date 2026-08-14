package io.github.r4t2.nilum.common.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes each bone's (BbOutlinerGroup) world transform at a point in time, cascaded down the
 * outliner hierarchy. With no animation, every bone resolves to identity relative to its parent.
 */
public final class BbBonePose {

    private static final BbVector3 UNIT_SCALE = new BbVector3(1, 1, 1);

    private BbBonePose() {
    }

    public static Map<String, BbMatrix4> compute(BbModel model, BbAnimation animation, double time) {
        Map<String, BbMatrix4> worldByBoneUuid = new HashMap<>();
        for (BbOutlinerNode node : model.outliner()) {
            if (node instanceof BbOutlinerGroup group) {
                computeGroup(group, animation, time, BbMatrix4.identity(), worldByBoneUuid);
            }
        }
        return worldByBoneUuid;
    }

    /** Always loops the model's first animation against wall-clock time; the pre-trigger default for AnimationPlaybackState. */
    public static Map<String, BbMatrix4> computeAutoLoop(BbModel model) {
        BbAnimation animation = model.animations().isEmpty() ? null : model.animations().get(0);
        double time = animation == null || animation.length() <= 0 ? 0
                : (System.nanoTime() / 1_000_000_000.0) % animation.length();
        return compute(model, animation, time);
    }

    private static void computeGroup(BbOutlinerGroup group, BbAnimation animation, double time,
                                      BbMatrix4 parentWorld, Map<String, BbMatrix4> out) {
        float ox = (float) (group.origin().x() / 16.0);
        float oy = (float) (group.origin().y() / 16.0);
        float oz = (float) (group.origin().z() / 16.0);

        BbAnimator animator = animation == null ? null : animation.animatorsByBoneUuid().get(group.uuid());
        BbVector3 positionDelta = BbVector3.ZERO;
        BbVector3 rotationDelta = BbVector3.ZERO;
        BbVector3 scaleDelta = UNIT_SCALE;
        if (animator != null && "bone".equals(animator.type())) {
            positionDelta = BbAnimationEvaluator.evaluate(animator, "position", time, BbVector3.ZERO);
            rotationDelta = BbAnimationEvaluator.evaluate(animator, "rotation", time, BbVector3.ZERO);
            scaleDelta = BbAnimationEvaluator.evaluate(animator, "scale", time, UNIT_SCALE);
        }

        float dx = (float) (positionDelta.x() / 16.0);
        float dy = (float) (positionDelta.y() / 16.0);
        float dz = (float) (positionDelta.z() / 16.0);

        BbMatrix4 local = BbMatrix4.translation(ox + dx, oy + dy, oz + dz)
                .multiply(BbMatrix4.rotationXYZDegrees((float) rotationDelta.x(), (float) rotationDelta.y(), (float) rotationDelta.z()))
                .multiply(BbMatrix4.scale((float) scaleDelta.x(), (float) scaleDelta.y(), (float) scaleDelta.z()))
                .multiply(BbMatrix4.translation(-ox, -oy, -oz));

        BbMatrix4 world = parentWorld.multiply(local);
        out.put(group.uuid(), world);

        for (BbOutlinerNode child : group.children()) {
            if (child instanceof BbOutlinerGroup childGroup) {
                computeGroup(childGroup, animation, time, world, out);
            }
        }
    }
}
