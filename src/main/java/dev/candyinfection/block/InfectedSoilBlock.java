package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Infected grass / dirt. Acts as the soil for all candy vegetation and slowly
 * converts plain dirt and grass around it.
 */
public class InfectedSoilBlock extends CandyBlock {
    public static final MapCodec<InfectedSoilBlock> CODEC = createCodec(InfectedSoilBlock::new);

    public InfectedSoilBlock(Settings settings) {
        super(settings, 1.35F);
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (random.nextInt(8) != 0) {
            return;
        }
        // Grow candy vegetation on top of infected soil.
        BlockPos above = pos.up();
        if (world.isAir(above) && random.nextBoolean()) {
            BlockState plant = CandyBlocks.randomVegetation(random);
            if (plant != null && plant.canPlaceAt(world, above)) {
                world.setBlockState(above, plant, Block.NOTIFY_ALL);
            }
        }
        // Occasionally thicken the colony sideways.
        BlockPos target = pos.add(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
        if (world.getBlockState(target).isOf(net.minecraft.block.Blocks.DIRT)
                || world.getBlockState(target).isOf(net.minecraft.block.Blocks.GRASS_BLOCK)) {
            world.setBlockState(target, CandyBlocks.INFECTED_DIRT.getDefaultState(), Block.NOTIFY_ALL);
        }
    }


    /** Infected soil always accepts candy plants. */
    public static boolean isCandySoil(BlockView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isOf(CandyBlocks.INFECTED_GRASS_BLOCK) || state.isOf(CandyBlocks.INFECTED_DIRT);
    }
}
