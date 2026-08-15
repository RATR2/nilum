package io.github.r4t2.nilum.neoforge.block;

import com.mojang.serialization.MapCodec;
import io.github.r4t2.nilum.common.model.BbCollisionBox;
import io.github.r4t2.nilum.common.model.CollisionResolver;
import io.github.r4t2.nilum.common.model.CollisionTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A single real, registered block backing every Nilum block definition on a NeoForge-hosted
 * server; which definition a given placement is comes from its NilumBlockEntity, not from a
 * separate registered Block per definition, so new definitions place without a restart.
 */
public final class NilumBlock extends Block implements EntityBlock {

    public static final MapCodec<NilumBlock> CODEC = simpleCodec(NilumBlock::new);

    public NilumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NilumBlockEntity(pos, state);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = level.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity ? blockEntity.hardness() : 1.0f;
        if (hardness < 0) {
            return 0.0f;
        }
        int denominator = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / hardness / denominator;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity ? blockEntity.collisionShape() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state, level, pos, context);
    }

    /** SOLID boxes are real collision via getCollisionShape; PARTIAL boxes dampen movement here instead, matching cobweb's own hook. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                 InsideBlockEffectApplier effectApplier, boolean isFalling) {
        if (!(level.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity)) {
            return;
        }
        for (BbCollisionBox box : blockEntity.collisionBoxes()) {
            if (box.tier() != CollisionTier.PARTIAL) {
                continue;
            }
            AABB worldBox = new AABB(pos.getX() + box.minX(), pos.getY() + box.minY(), pos.getZ() + box.minZ(),
                    pos.getX() + box.maxX(), pos.getY() + box.maxY(), pos.getZ() + box.maxZ());
            if (entity.getBoundingBox().intersects(worldBox)) {
                double factor = CollisionResolver.PARTIAL_MOVEMENT_FACTOR;
                entity.makeStuckInBlock(state, new Vec3(factor, factor, factor));
                return;
            }
        }
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().noOcclusion().strength(1.0f, 6.0f);
    }
}
