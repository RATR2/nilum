package io.github.r4t2.nilum.fabric.render;

import io.github.r4t2.nilum.common.asset.ClientModelStore;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a (possibly already icon-wrapped) {@link ItemModel}. Checks the actual stack instance for
 * a Nilum full-model tag first - if present and loaded, draws the model's real baked geometry
 * using its own authored Blockbench Display panel data (falling back to vanilla's generated-item
 * defaults per context/field it doesn't specify); otherwise delegates to the wrapped model. Since
 * icon and model tags are mutually exclusive per item, wrapping order between the two doesn't
 * matter - whichever tag a stack actually carries is the one that fires.
 */
public final class NilumModelItemModel implements ItemModel {

    private final ItemModel original;
    private final ClientModelStore modelStore;
    private final NilumModelItemSpecialRenderer modelRenderer;

    public NilumModelItemModel(ItemModel original, ClientModelStore modelStore, NilumModelItemSpecialRenderer modelRenderer) {
        this.original = original;
        this.modelStore = modelStore;
        this.modelRenderer = modelRenderer;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack itemStack, ItemModelResolver resolver,
                        ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        String modelId = NilumItemTags.get(itemStack, "nilum:model_id");
        // See NilumIconItemModel - without this, GUI-slot render-state caching can't tell stacks
        // that resolve to different models (or a model vs. an icon) apart.
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(modelId == null ? "no-model" : modelId);

        if (modelId != null) {
            var model = modelStore.model(modelId).orElse(null);
            if (model != null) {
                ItemStackRenderState.LayerRenderState layer = state.newLayer();
                layer.setTransform(NilumDisplayTransforms.resolve(model, displayContext.getSerializedName()));
                // Without this, LayerRenderState.extents stays at its default empty-array supplier,
                // ItemStackRenderState.getModelBoundingBox() ends up with minY=+Infinity, and
                // ItemEntityRenderer translates dropped items by -Infinity - invisible. See vanilla's
                // own SpecialModelWrapper.update(), which does the same setExtents(...) call.
                layer.setExtents(() -> modelRenderer.extentsOf(modelId));
                layer.setupSpecialModel(modelRenderer, modelId);
                return;
            }
        }

        original.update(state, itemStack, resolver, displayContext, level, owner, seed);
    }
}
