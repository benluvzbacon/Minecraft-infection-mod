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
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        // More frequent cleanup - was 1/8, now 1/3 for cleaner look
        if (random.nextInt(3) != 0) {
            // Still grow vegetation sometimes even when not doing full cleanup
            if (random.nextInt(4) == 0) {
                BlockPos above = pos.up();
                if (world.isAir(above) && random.nextBoolean()) {
                    BlockState plant = CandyBlocks.randomVegetation(random);
                    if (plant != null && plant.canPlaceAt(world, above)) {
                        world.setBlockState(above, plant, Block.NOTIFY_ALL);
                    }
                }
            }
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
        // CLEANUP: Thicken colony and clean up leftover grass - now checks 4 positions per tick instead of 1
        // This makes infection look cleaner without instant-converting everything (budgeted)
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.add(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            var targetState = world.getBlockState(target);
            var targetBlock = targetState.getBlock();
            // Convert various vanilla soils to infected versions
            if (targetBlock == net.minecraft.block.Blocks.DIRT) {
                world.setBlockState(target, CandyBlocks.INFECTED_DIRT.getDefaultState(), Block.NOTIFY_ALL);
            } else if (targetBlock == net.minecraft.block.Blocks.GRASS_BLOCK || targetBlock == net.minecraft.block.Blocks.MYCELIUM) {
                world.setBlockState(target, CandyBlocks.INFECTED_GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            } else if (targetBlock == net.minecraft.block.Blocks.COARSE_DIRT || targetBlock == net.minecraft.block.Blocks.PODZOL ||
                       targetBlock == net.minecraft.block.Blocks.ROOTED_DIRT || targetBlock == net.minecraft.block.Blocks.MOSS_BLOCK) {
                world.setBlockState(target, CandyBlocks.INFECTED_DIRT.getDefaultState(), Block.NOTIFY_ALL);
            } else if (targetBlock == net.minecraft.block.Blocks.STONE || targetBlock == net.minecraft.block.Blocks.COBBLESTONE) {
                if (random.nextInt(2) == 0) {
                    world.setBlockState(target, CandyBlocks.CANDY_STONE.getDefaultState(), Block.NOTIFY_ALL);
                }
            } else if (targetBlock == net.minecraft.block.Blocks.SAND || targetBlock == net.minecraft.block.Blocks.RED_SAND) {
                world.setBlockState(target, CandyBlocks.CANDY_SAND.getDefaultState(), Block.NOTIFY_ALL);
            } else if (targetBlock == net.minecraft.block.Blocks.GRAVEL) {
                world.setBlockState(target, CandyBlocks.CANDY_GRAVEL.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }


    /** Infected soil always accepts candy plants. */
    public static boolean isCandySoil(BlockView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isOf(CandyBlocks.INFECTED_GRASS_BLOCK) || state.isOf(CandyBlocks.INFECTED_DIRT);
    }
}
