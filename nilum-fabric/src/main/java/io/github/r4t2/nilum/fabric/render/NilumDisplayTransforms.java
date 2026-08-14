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

    /** Resolves a full .bbmodel's Display panel data for one context, falling back per-field to identity (not vanilla's generated-item defaults). */
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

    /** Matches vanilla's ItemTransform.Deserializer: translation scaled by 1/16 and clamped to [-5, 5] (scale clamped to [-4, 4]). */
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
