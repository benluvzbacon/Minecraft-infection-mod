package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Base class for every block produced by the infection.
 *
 * <p>Instead of scanning chunks, infected blocks only participate in the spread
 * simulation when they receive a random tick: they hand a bounded number of
 * candidate positions to {@link InfectionSpread}, which enforces a global per
 * tick budget. This keeps huge infected regions cheap.
 */
public class CandyBlock extends Block {
    public static final MapCodec<CandyBlock> CODEC = createCodec(CandyBlock::new);

    /** Multiplier applied to the global spread chance for this block type. */
    private final float spreadMultiplier;

    public CandyBlock(Settings settings) {
        this(settings, 1.0F);
    }

    public CandyBlock(Settings settings, float spreadMultiplier) {
        super(settings);
        this.spreadMultiplier = spreadMultiplier;
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }

    public float getSpreadMultiplier() {
        return this.spreadMultiplier;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        InfectionSpread.onInfectedBlockTick(world, pos, state, this.spreadMultiplier);
    }

    @Override
    protected void onBlockAdded(BlockState state, net.minecraft.world.World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient && !oldState.isOf(this)) {
            InfectionSpread.onInfectedBlockPlaced(world, pos, state);
        }
    }
}
