package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Gummy leaves. Unlike normal leaves these never decay - the infection keeps
 * them alive - and they drop gummy resin instead of saplings.
 */
public class GummyLeavesBlock extends CandyBlock {
    public static final MapCodec<GummyLeavesBlock> CODEC = createCodec(GummyLeavesBlock::new);

    public GummyLeavesBlock(AbstractBlock.Settings settings) {
        super(settings.nonOpaque(), 0.7F);
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (random.nextInt(12) == 0) {
            // Leaves seed new candy growth on the ground below them.
            for (int i = 1; i < 6; i++) {
                BlockPos below = pos.down(i);
                if (!world.isAir(below)) {
                    if (world.isAir(pos.down(i - 1))
                            && (world.getBlockState(below).isOf(CandyBlocks.INFECTED_GRASS_BLOCK)
                            || world.getBlockState(below).isOf(CandyBlocks.INFECTED_DIRT))) {
                        BlockState plant = CandyBlocks.randomVegetation(random);
                        if (plant != null && plant.canPlaceAt(world, pos.down(i - 1))) {
                            world.setBlockState(pos.down(i - 1), plant, Block.NOTIFY_ALL);
                        }
                    }
                    break;
                }
            }
        }
    }

    /** Called when a leaf block is converted from vanilla leaves. */
    public static void afterConversion(ServerWorld world, BlockPos pos) {
        InfectionSpread.onInfectedBlockPlaced(world, pos, CandyBlocks.GUMMY_LEAVES.getDefaultState());
    }
}
