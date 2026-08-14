package io.github.r4t2.nilum.fabric.render;

import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbBakedVertex;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Wraps an ItemModel. Draws the model's baked geometry if the stack carries a Nilum full-model tag, otherwise delegates to the wrapped model. */
public final class NilumModelItemModel implements ItemModel {

    private final ItemModel original;
    private final ClientModelStore modelStore;
    private final NilumModelItemSpecialRenderer modelRenderer;
    private final NilumGlintSpecialRenderer glintRenderer;

    public NilumModelItemModel(ItemModel original, ClientModelStore modelStore, NilumModelItemSpecialRenderer modelRenderer,
                                NilumGlintSpecialRenderer glintRenderer) {
        this.original = original;
        this.modelStore = modelStore;
        this.modelRenderer = modelRenderer;
        this.glintRenderer = glintRenderer;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack itemStack, ItemModelResolver resolver,
                        ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        String modelId = NilumItemTags.get(itemStack, "nilum:model_id");
        // See NilumIconItemModel; without this, GUI-slot render-state caching can't tell stacks
        // that resolve to different models (or a model vs. an icon) apart.
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(modelId == null ? "no-model" : modelId);

        if (modelId != null) {
            var model = modelStore.model(modelId).orElse(null);
            if (model != null) {
                var itemTransform = NilumDisplayTransforms.resolve(model, displayContext.getSerializedName());

                ItemStackRenderState.LayerRenderState layer = state.newLayer();
                layer.setTransform(itemTransform);
                // Without this, LayerRenderState.extents stays at its default empty-array supplier,
                // ItemStackRenderState.getModelBoundingBox() ends up with minY=+Infinity, and
                // ItemEntityRenderer translates dropped items by -Infinity, making them invisible.
                // See vanilla's own SpecialModelWrapper.update(), which does the same setExtents(...) call.
                layer.setExtents(() -> modelRenderer.extentsOf(modelId));
                layer.setupSpecialModel(modelRenderer, modelId);

                GlintRenderData glint = GlintTagReader.read(itemStack, glintQuadsOf(modelId));
                if (glint != null) {
                    // Same baked quads and same per-context transform as the model layer above,
                    // so the glint is masked to the model's real shape and tracks its exact
                    // positioning instead of floating as an unrelated overlay.
                    ItemStackRenderState.LayerRenderState glintLayer = state.newLayer();
                    glintLayer.setTransform(itemTransform);
                    glintLayer.setExtents(() -> modelRenderer.extentsOf(modelId));
                    glintLayer.setupSpecialModel(glintRenderer, glint);
                }
                return;
            }
        }

        original.update(state, itemStack, resolver, displayContext, level, owner, seed);
    }

    private List<GlintQuad> glintQuadsOf(String modelId) {
        var quadsByTexture = modelStore.bakedQuadsByTexture(modelId).orElse(null);
        if (quadsByTexture == null) {
            return List.of();
        }
        List<GlintQuad> glintQuads = new ArrayList<>();
        for (List<BbBakedQuad> quads : quadsByTexture.values()) {
            for (BbBakedQuad quad : quads) {
                glintQuads.add(new GlintQuad(
                        toGlintVertex(quad.v0()), toGlintVertex(quad.v1()),
                        toGlintVertex(quad.v2()), toGlintVertex(quad.v3())));
            }
        }
        return glintQuads;
    }

    private static GlintQuad.GlintVertex toGlintVertex(BbBakedVertex vertex) {
        return new GlintQuad.GlintVertex(vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v());
    }
}
