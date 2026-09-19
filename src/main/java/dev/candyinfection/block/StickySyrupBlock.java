package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyEffects;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Sticky syrup: a semi-solid puddle of sugar. It slows everything that walks
 * through it and eats fall damage, which makes syrup flats feel deceptively
 * safe right before a candy monster closes the distance.
 */
public class StickySyrupBlock extends CandyBlock {
    public static final MapCodec<StickySyrupBlock> CODEC = createCodec(StickySyrupBlock::new);
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public StickySyrupBlock(AbstractBlock.Settings settings) {
        super(settings.velocityMultiplier(0.4F).jumpVelocityMultiplier(0.6F).nonOpaque(), 0.6F);
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        entity.slowMovement(state, new net.minecraft.util.math.Vec3d(0.35D, 0.05D, 0.35D));
        if (!world.isClient && entity instanceof LivingEntity living && living.age % 40 == 0) {
            living.addStatusEffect(new StatusEffectInstance(CandyEffects.STICKY, 100, 0, false, true));
        }
    }

    @Override
    protected void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // Syrup is soft - falling into it barely hurts.
        entity.handleFallDamage(fallDistance, 0.1F, world.getDamageSources().fall());
    }

    @Override
    protected void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof PlayerEntity player && player.age % 60 == 0) {
            player.addStatusEffect(new StatusEffectInstance(CandyEffects.STICKY, 80, 0, false, true));
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
