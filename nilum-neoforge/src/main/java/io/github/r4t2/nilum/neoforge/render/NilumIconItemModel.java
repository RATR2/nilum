package io.github.r4t2.nilum.neoforge.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps a vanilla per-item-id baked ItemModel. If the stack carries a Nilum icon tag loaded in
 * the shared atlas, draws that instead; otherwise delegates to the wrapped model.
 */
public final class NilumIconItemModel implements ItemModel {

    private static final List<GlintQuad> ICON_GLINT_QUADS = List.of(new GlintQuad(
            new GlintQuad.GlintVertex(0f, 0f, 0.5f, 0f, 0f),
            new GlintQuad.GlintVertex(1f, 0f, 0.5f, 1f, 0f),
            new GlintQuad.GlintVertex(1f, 1f, 0.5f, 1f, 1f),
            new GlintQuad.GlintVertex(0f, 1f, 0.5f, 0f, 1f)));

    private final ItemModel original;
    private final IconAtlas iconAtlas;
    private final NilumIconSpecialRenderer iconRenderer;
    private final NilumGlintSpecialRenderer glintRenderer;

    public NilumIconItemModel(ItemModel original, IconAtlas iconAtlas, NilumIconSpecialRenderer iconRenderer,
                               NilumGlintSpecialRenderer glintRenderer) {
        this.original = original;
        this.iconAtlas = iconAtlas;
        this.iconRenderer = iconRenderer;
        this.glintRenderer = glintRenderer;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack itemStack, ItemModelResolver resolver,
                        ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        String iconId = NilumItemTags.get(itemStack, "nilum:icon_id");
        // Vanilla's own data-dependent ItemModels (e.g. ConditionalItemModel) append an identity
        // element for whatever they branched on, so GUI-slot render-state caching can tell two
        // stacks that resolve differently apart. Without this, two different icon ids sharing a
        // base material would look identical to that cache and one could serve the other's render.
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(iconId == null ? "no-icon" : iconId);

        if (iconId != null && iconAtlas.uvOf(iconId).isPresent()) {
            var itemTransform = iconAtlas.displayOf(iconId)
                    .map(display -> display.transformFor(displayContext.getSerializedName()))
                    .map(transform -> NilumDisplayTransforms.toItemTransform(
                            transform.rotation(), transform.translation(), transform.scale()))
                    .orElse(null);

            ItemStackRenderState.LayerRenderState layer = state.newLayer();
            if (itemTransform != null) {
                layer.setTransform(itemTransform);
            }
            // Without this, LayerRenderState.extents stays at its default empty-array supplier,
            // ItemStackRenderState.getModelBoundingBox() ends up with minY=+Infinity, and
            // ItemEntityRenderer translates dropped items by -Infinity, making them invisible.
            // See vanilla's own SpecialModelWrapper.update(), which does the same setExtents(...) call.
            layer.setExtents(() -> NilumIconSpecialRenderer.EXTENTS);
            layer.setupSpecialModel(iconRenderer, iconId);

            GlintRenderData glint = GlintTagReader.read(itemStack, ICON_GLINT_QUADS);
            if (glint != null) {
                // Same quad shape and same per-context transform as the icon layer above, so the
                // glint is masked to the icon's real shape and tracks its exact positioning
                // instead of floating as an unrelated overlay.
                ItemStackRenderState.LayerRenderState glintLayer = state.newLayer();
                if (itemTransform != null) {
                    glintLayer.setTransform(itemTransform);
                }
                glintLayer.setExtents(() -> NilumIconSpecialRenderer.EXTENTS);
                glintLayer.setupSpecialModel(glintRenderer, glint);
            }
            return;
        }

        original.update(state, itemStack, resolver, displayContext, level, owner, seed);
    }
}
