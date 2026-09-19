package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Generic candy vegetation: gummy growths, chocolate growth, lollipop sprouts.
 * Requires infected soil beneath it and spreads the infection around itself.
 */
public class CandyPlantBlock extends CandyBlock {
    public static final MapCodec<CandyPlantBlock> CODEC = createCodec(CandyPlantBlock::new);
    public static final IntProperty AGE = IntProperty.of("age", 0, 2);

    protected static final VoxelShape SMALL = Block.createCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);
    protected static final VoxelShape MEDIUM = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);
    protected static final VoxelShape FULL = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D);

    private final float spreadMultiplier;

    public CandyPlantBlock(Settings settings) {
        this(settings, 1.2F);
    }

    public CandyPlantBlock(Settings settings, float spreadMultiplier) {
        super(settings.offset(AbstractBlock.OffsetType.XZ).noCollision().ticksRandomly(), spreadMultiplier);
        this.spreadMultiplier = spreadMultiplier;
        this.setDefaultState(this.getStateManager().getDefaultState().with(AGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(AGE)) {
            case 0 -> SMALL;
            case 1 -> MEDIUM;
            default -> FULL;
        };
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return InfectedSoilBlock.isCandySoil(world, pos)
                || floor.isOf(CandyBlocks.CANDY_SAND)
                || floor.isOf(Blocks.DIRT)
                || floor.isOf(Blocks.GRASS_BLOCK)
                || floor.isOf(Blocks.MOSS_BLOCK);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos below = pos.down();
        return this.canPlantOnTop(world.getBlockState(below), world, below) && world.getBaseLightLevel(pos, 0) >= 0;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, net.minecraft.util.math.Direction direction,
                                                   BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (state.get(AGE) < 2 && random.nextInt(4) == 0) {
            world.setBlockState(pos, state.with(AGE, state.get(AGE) + 1), Block.NOTIFY_ALL);
        }
    }


    /** Drops extra material once fully grown. */
    public boolean isFullyGrown(BlockState state) {
        return state.get(AGE) >= 2;
    }

    /** Hook used by the infection simulation to keep plants alive in dark caves. */
    public static void tickSpread(ServerWorld world, BlockPos pos) {
        InfectionSpread.onInfectedBlockTick(world, pos, world.getBlockState(pos), 1.0F);
    }
}
