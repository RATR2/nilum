package io.github.r4t2.nilum.neoforge.block;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Wraps every baked block model. Suppresses a Nilum block's vanilla appearance entirely (no
 * parts); NilumBlockRenderer draws the real geometry separately. Collision/hardness are untouched.
 */
public final class NilumBlockStateModel implements BlockStateModel {

    private final BlockStateModel original;
    private final ClientBlockRegistry blockRegistry;

    public NilumBlockStateModel(BlockStateModel original, ClientBlockRegistry blockRegistry) {
        this.original = original;
        this.blockRegistry = blockRegistry;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        // No BlockPos available here (vanilla's legacy interface is position-independent); only
        // reached for non-terrain uses (item frames, etc.), not the real per-position terrain
        // path below. Pass through untouched.
        original.collectParts(random, parts);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
        if (blockRegistry.modelIdAt(pos).isPresent()) {
            return;
        }
        original.collectParts(level, pos, state, random, parts);
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return original.particleIcon();
    }

    @Override
    public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return original.particleIcon(level, pos, state);
    }
}
