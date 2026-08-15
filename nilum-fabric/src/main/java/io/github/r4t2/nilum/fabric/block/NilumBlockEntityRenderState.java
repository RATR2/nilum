package io.github.r4t2.nilum.fabric.block;

import io.github.r4t2.nilum.common.model.AnimationPlaybackState;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.List;
import java.util.Map;

/** Adds the resolved Nilum model, if any, onto the vanilla block entity render state. */
public final class NilumBlockEntityRenderState extends BlockEntityRenderState {

    public String nilumModelId;
    public BbModel nilumModel;
    public Map<String, List<BbBakedQuad>> nilumQuadsByBone;
    public AnimationPlaybackState nilumAnimationState;
}
