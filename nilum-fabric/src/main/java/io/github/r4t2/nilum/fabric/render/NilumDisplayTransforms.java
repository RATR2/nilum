package io.github.r4t2.nilum.fabric.render;

import io.github.r4t2.nilum.common.model.BbDisplayTransform;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbVector3;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.Optional;

/** Shared BbVector3/BbDisplayTransform -> real ItemTransform conversion, used by both icon and full-model item rendering. */
public final class NilumDisplayTransforms {

    private NilumDisplayTransforms() {
    }

    /**
     * Resolves a full {@code .bbmodel}'s own Blockbench Display panel data for one context,
     * falling back per-field to identity - no config file involved, unlike icons.yml, since a
     * full model already authors this in Blockbench. Deliberately does NOT fall back to
     * vanilla's generated-item defaults (unlike the icon-atlas path): those numbers (e.g. the
     * firstperson_righthand (0,-90,25) rotation) exist to lay vanilla's flat single-quad
     * "generated" icon flat to face the camera, and reorienting a real 3D Blockbench mesh with
     * them produces a wrong, hand-specific rotation instead of the intended no-op.
     */
    public static ItemTransform resolve(BbModel model, String context) {
        BbDisplayTransform authored = model.display().get(context);

        BbVector3 rotation = fieldOrElse(authored == null ? null : authored.rotation(), BbVector3.ZERO);
        BbVector3 translation = fieldOrElse(authored == null ? null : authored.translation(), BbVector3.ZERO);
        BbVector3 scale = fieldOrElse(authored == null ? null : authored.scale(), new BbVector3(1, 1, 1));

        return toItemTransform(rotation, translation, scale);
    }

    private static BbVector3 fieldOrElse(Optional<BbVector3> authored, BbVector3 identity) {
        if (authored != null && authored.isPresent()) {
            return authored.get();
        }
        return identity;
    }

    /**
     * Rotation/translation/scale here are in the same raw units Blockbench's Display panel (and
     * vanilla's own model JSON) use. Vanilla's own ItemTransform.Deserializer scales translation
     * by 1/16 and clamps it to [-5, 5] (scale to [-4, 4]) when loading from JSON - reproduced
     * here since we build ItemTransform directly rather than going through that deserializer.
     */
    public static ItemTransform toItemTransform(BbVector3 rotation, BbVector3 translation, BbVector3 scale) {
        Vector3f rotationVec = toVector3f(rotation);
        Vector3f translationVec = toVector3f(translation);
        translationVec.mul(0.0625f);
        translationVec.set(Mth.clamp(translationVec.x, -5f, 5f), Mth.clamp(translationVec.y, -5f, 5f), Mth.clamp(translationVec.z, -5f, 5f));
        Vector3f scaleVec = toVector3f(scale);
        scaleVec.set(Mth.clamp(scaleVec.x, -4f, 4f), Mth.clamp(scaleVec.y, -4f, 4f), Mth.clamp(scaleVec.z, -4f, 4f));
        return new ItemTransform(rotationVec, translationVec, scaleVec);
    }

    private static Vector3f toVector3f(BbVector3 vector) {
        return new Vector3f((float) vector.x(), (float) vector.y(), (float) vector.z());
    }
}
