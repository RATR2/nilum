package io.github.r4t2.nilum.fabric.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a vanilla per-item-id baked {@link ItemModel}. Checks the actual stack instance for
 * a Nilum icon tag first - if present and loaded in the shared atlas, draws that instead;
 * otherwise delegates untouched to the wrapped model. Registered once per item id via
 * {@code ModelLoadingPlugin}'s after-bake hook, so this runs for every item type, but only
 * ever diverges from vanilla behavior for stacks actually carrying our tag.
 */
public final class NilumIconItemModel implements ItemModel {

    private final ItemModel original;
    private final IconAtlas iconAtlas;
    private final NilumIconSpecialRenderer iconRenderer;

    public NilumIconItemModel(ItemModel original, IconAtlas iconAtlas, NilumIconSpecialRenderer iconRenderer) {
        this.original = original;
        this.iconAtlas = iconAtlas;
        this.iconRenderer = iconRenderer;
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
            ItemStackRenderState.LayerRenderState layer = state.newLayer();
            iconAtlas.displayOf(iconId).ifPresent(display -> {
                var transform = display.transformFor(displayContext.getSerializedName());
                layer.setTransform(NilumDisplayTransforms.toItemTransform(
                        transform.rotation(), transform.translation(), transform.scale()));
            });
            // Without this, LayerRenderState.extents stays at its default empty-array supplier,
            // ItemStackRenderState.getModelBoundingBox() ends up with minY=+Infinity, and
            // ItemEntityRenderer translates dropped items by -Infinity - invisible. See vanilla's
            // own SpecialModelWrapper.update(), which does the same setExtents(...) call.
            layer.setExtents(() -> NilumIconSpecialRenderer.EXTENTS);
            layer.setupSpecialModel(iconRenderer, iconId);
            return;
        }

        original.update(state, itemStack, resolver, displayContext, level, owner, seed);
    }
}
