package io.github.r4t2.nilum.neoforge.block;

import io.github.r4t2.nilum.common.model.BbCollisionBox;
import io.github.r4t2.nilum.common.model.CollisionBoxSerializer;
import io.github.r4t2.nilum.common.model.CollisionTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** A real Nilum block's per-position data: which definition it is, plus the hardness/resistance/collision baked in at placement time. */
public final class NilumBlockEntity extends BlockEntity {

    private String definitionId = "";
    private float hardness = 1.0f;
    private float explosionResistance = 1.0f;
    private List<BbCollisionBox> collisionBoxes = List.of();
    private VoxelShape collisionShape = Shapes.block();

    public NilumBlockEntity(BlockPos pos, BlockState state) {
        super(NilumBlocks.BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    public void setDefinition(String definitionId, float hardness, float explosionResistance, List<BbCollisionBox> collisionBoxes) {
        this.definitionId = definitionId;
        this.hardness = hardness;
        this.explosionResistance = explosionResistance;
        setCollisionBoxes(collisionBoxes);
        setChanged();
    }

    private void setCollisionBoxes(List<BbCollisionBox> boxes) {
        this.collisionBoxes = boxes;
        this.collisionShape = buildShape(boxes);
    }

    private static VoxelShape buildShape(List<BbCollisionBox> boxes) {
        VoxelShape shape = Shapes.empty();
        for (BbCollisionBox box : boxes) {
            if (box.tier() != CollisionTier.SOLID) {
                continue;
            }
            shape = Shapes.or(shape, Shapes.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        }
        return shape;
    }

    public String definitionId() {
        return definitionId;
    }

    public float hardness() {
        return hardness;
    }

    public float explosionResistance() {
        return explosionResistance;
    }

    public List<BbCollisionBox> collisionBoxes() {
        return collisionBoxes;
    }

    public VoxelShape collisionShape() {
        return collisionShape;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("definition_id", definitionId);
        output.putFloat("hardness", hardness);
        output.putFloat("explosion_resistance", explosionResistance);
        output.putString("collision_boxes", CollisionBoxSerializer.encode(collisionBoxes));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        definitionId = input.getStringOr("definition_id", "");
        hardness = input.getFloatOr("hardness", 1.0f);
        explosionResistance = input.getFloatOr("explosion_resistance", 1.0f);
        setCollisionBoxes(CollisionBoxSerializer.decode(input.getStringOr("collision_boxes", "")));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        saveAdditional(output);
        return output.buildResult();
    }
}
