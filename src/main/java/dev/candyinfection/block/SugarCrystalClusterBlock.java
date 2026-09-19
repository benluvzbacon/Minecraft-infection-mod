package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * A cluster of glowing sugar crystals that grows out of the ground. Grows in
 * three size steps and drops sugar shards when broken at full size.
 */
public class SugarCrystalClusterBlock extends CandyBlock {
    public static final MapCodec<SugarCrystalClusterBlock> CODEC = createCodec(SugarCrystalClusterBlock::new);
    public static final IntProperty SIZE = IntProperty.of("size", 0, 2);

    private static final VoxelShape SMALL = Block.createCuboidShape(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
    private static final VoxelShape MEDIUM = Block.createCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 11.0D, 12.0D);
    private static final VoxelShape LARGE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public SugarCrystalClusterBlock(AbstractBlock.Settings settings) {
        super(settings.noCollision().ticksRandomly(), 0.8F);
        this.setDefaultState(this.getStateManager().getDefaultState().with(SIZE, 0));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SIZE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(SIZE)) {
            case 0 -> SMALL;
            case 1 -> MEDIUM;
            default -> LARGE;
        };
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos below = pos.down();
        BlockState floor = world.getBlockState(below);
        return floor.isSideSolidFullSquare(world, below, Direction.UP) || floor.isOf(this);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        int size = state.get(SIZE);
        if (size < 2 && random.nextInt(3) == 0) {
            world.setBlockState(pos, state.with(SIZE, size + 1), Block.NOTIFY_ALL);
            return;
        }
        if (size >= 2 && random.nextInt(10) == 0) {
            BlockPos above = pos.up();
            if (world.isAir(above) && state.canPlaceAt(world, above)) {
                world.setBlockState(above, this.getDefaultState().with(SIZE, 0), Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    protected ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(CandyItems.SUGAR_SHARD);
    }
}
