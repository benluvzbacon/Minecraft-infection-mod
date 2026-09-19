package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * A giant lollipop: a sugar stick with a candy disc on top. Purely decorative
 * but a very clear "this land is already lost" landmark.
 */
public class LollipopBlock extends CandyBlock {
    public static final MapCodec<LollipopBlock> CODEC = createCodec(LollipopBlock::new);

    private static final VoxelShape STICK = Block.createCuboidShape(7.0D, 0.0D, 7.0D, 9.0D, 12.0D, 9.0D);
    private static final VoxelShape DISC = Block.createCuboidShape(3.0D, 12.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final VoxelShape SHAPE = VoxelShapes.union(STICK, DISC);

    public LollipopBlock(AbstractBlock.Settings settings) {
        super(settings.nonOpaque().ticksRandomly(), 0.3F);
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
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos below = pos.down();
        return world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (random.nextInt(20) == 0 && world.isAir(pos.up()) && this.canPlaceAt(state, world, pos.up())) {
            world.setBlockState(pos.up(), this.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
