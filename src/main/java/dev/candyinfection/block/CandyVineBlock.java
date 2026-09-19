package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Candy vines that cling to the side of a block, exactly like ivy growing over
 * an infected tree trunk. They creep downwards and outwards over time.
 */
public class CandyVineBlock extends CandyBlock {
    public static final MapCodec<CandyVineBlock> CODEC = createCodec(CandyVineBlock::new);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    private static final VoxelShape NORTH = Block.createCuboidShape(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D);
    private static final VoxelShape WEST = Block.createCuboidShape(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D);

    public CandyVineBlock(Settings settings) {
        super(settings.noCollision().ticksRandomly(), 0.9F);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> EAST;
        };
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockPos attached = pos.offset(facing.getOpposite());
        return world.getBlockState(attached).isSolidBlock(world, attached);
    }

    @Override
    protected BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = this.getDefaultState();
        for (Direction direction : ctx.getPlacementDirections()) {
            if (direction.getAxis().isHorizontal()) {
                state = state.with(FACING, direction.getOpposite());
                if (state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (random.nextInt(6) != 0) {
            return;
        }
        // Creep down the trunk.
        BlockPos below = pos.down();
        if (world.isAir(below) && world.getBlockState(pos.offset(state.get(FACING).getOpposite())).isSolidBlock(world, pos.offset(state.get(FACING).getOpposite()))) {
            world.setBlockState(below, this.getDefaultState().with(FACING, state.get(FACING)), Block.NOTIFY_ALL);
            return;
        }
        // Creep sideways onto a neighbouring face.
        Direction side = Direction.Type.HORIZONTAL.random(random);
        BlockPos target = pos.offset(side);
        if (world.isAir(target) && world.getBlockState(target.offset(side.getOpposite())).isSolidBlock(world, target.offset(side.getOpposite()))) {
            world.setBlockState(target, this.getDefaultState().with(FACING, side.getOpposite()), Block.NOTIFY_ALL);
        }
    }

    /** Used by worldgen helpers to place vines on a trunk. */
    public static void placeOn(ServerWorld world, BlockPos trunkPos, Random random) {
        for (Direction direction : Direction.Type.HORIZONTAL)
            for (BlockPos pos : BlockPos.iterate(trunkPos.offset(direction), trunkPos.offset(direction).up(random.nextInt(3) + 1))) {
                if (world.isAir(pos)) {
                    world.setBlockState(pos, CandyBlocks.CANDY_VINES.getDefaultState().with(FACING, direction.getOpposite()), Block.NOTIFY_ALL);
                    InfectionSpread.onInfectedBlockPlaced(world, pos, world.getBlockState(pos));
                }
            }
    }
}
