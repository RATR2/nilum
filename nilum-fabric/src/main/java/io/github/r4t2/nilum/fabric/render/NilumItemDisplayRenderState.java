package io.github.r4t2.nilum.fabric.render;

import io.github.r4t2.nilum.common.model.AnimationPlaybackState;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbModel;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;

import java.util.List;
import java.util.Map;

/** Adds the resolved Nilum model, if any, onto the vanilla render state. */
public final class NilumItemDisplayRenderState extends ItemDisplayEntityRenderState {

    public String nilumModelId;
    public BbModel nilumModel;
    public Map<String, List<BbBakedQuad>> nilumQuadsByBone;
    public AnimationPlaybackState nilumAnimationState;

    /** Vanilla's DisplayRenderer.submit() only calls submitInner() when this returns true; without it, submitInner() is never reached. */
    @Override
    public boolean hasSubState() {
        return super.hasSubState() || nilumModel != null;
    }
}
