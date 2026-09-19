package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyEffects;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Hardened caramel. Extremely tough, resistant to fire and it clings to anyone
 * who walks over it.
 */
public class CaramelBlock extends CandyBlock {
    public static final MapCodec<CaramelBlock> CODEC = createCodec(CaramelBlock::new);

    public CaramelBlock(AbstractBlock.Settings settings) {
        super(settings.velocityMultiplier(0.55F).jumpVelocityMultiplier(0.8F), 0.5F);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity living && living.age % 30 == 0) {
            living.addStatusEffect(new StatusEffectInstance(CandyEffects.CARAMEL_COATED, 120, 0, false, true));
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
