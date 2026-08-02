package io.github.r4t2.nilum.fabric.block;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * Wraps every baked block model. If the position being rendered is a registered Nilum block,
 * suppresses the proxy's vanilla appearance entirely (no quads at all); the real visible
 * geometry is drawn separately by NilumBlockRenderer, since block-terrain quads can only sample
 * vanilla's block/item atlases. Hiding the render here doesn't touch collision or hardness,
 * those come from BlockState, so the block stays mechanically "real" the whole time.
 */
public final class NilumBlockStateModel implements BlockStateModel, FabricBlockStateModel {

    private final BlockStateModel original;
    private final ClientBlockRegistry blockRegistry;

    public NilumBlockStateModel(BlockStateModel original, ClientBlockRegistry blockRegistry) {
        this.original = original;
        this.blockRegistry = blockRegistry;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        // No BlockPos available here (vanilla's own interface is position-independent); only
        // reached for non-terrain uses (item frames, etc.), not the real per-position terrain
        // path FRAPI-aware renderers like Sodium use. Pass through untouched.
        original.collectParts(random, parts);
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return original.particleIcon();
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state,
                           RandomSource random, Predicate<Direction> cullTest) {
        if (blockRegistry.modelIdAt(pos).isPresent()) {
            return;
        }

        if (original instanceof FabricBlockStateModel fabricOriginal) {
            fabricOriginal.emitQuads(emitter, blockView, pos, state, random, cullTest);
        } else {
            for (BlockModelPart part : original.collectParts(random)) {
                part.emitQuads(emitter, cullTest);
            }
        }
    }
}
