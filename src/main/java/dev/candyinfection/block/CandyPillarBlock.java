package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Infected wood: a gummy log that can be placed along any axis. */
public class CandyPillarBlock extends PillarBlock {
    public static final MapCodec<CandyPillarBlock> CODEC = createCodec(CandyPillarBlock::new);

    public CandyPillarBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<? extends PillarBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        InfectionSpread.onInfectedBlockTick(world, pos, state, 0.85F);
    }

    @Override
    protected void onBlockAdded(BlockState state, net.minecraft.world.World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient && !oldState.isOf(this)) {
            InfectionSpread.onInfectedBlockPlaced(world, pos, state);
        }
    }
}
