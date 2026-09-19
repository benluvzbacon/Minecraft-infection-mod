package dev.candyinfection.world.gen;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

/**
 * Grows the six flavours of Infected Land.
 *
 * <p>Regions are built chunk by chunk, lazily, only while a chunk is loaded and
 * only for chunks the infection has already reached - there is no whole-world
 * pass and no chunk scanning. Each call is budgeted so a chunk conversion is
 * spread across a handful of ticks instead of one long stall.
 */
public final class CandyRegionGenerator {
    /** How many blocks a single region tick is allowed to touch. */
    private static final int BUDGET_PER_TICK = 96;

    private CandyRegionGenerator() {
    }

    /**
     * Converts one column of the chunk at the given local x/z. Returns the number
     * of blocks changed, so the caller can charge it against the spread budget.
     */
    public static int growChunk(ServerWorld world, ChunkPos chunkPos, int localX, int localZ, InfectionWorldState stages) {
        CandyConfig config = CandyConfig.get();
        int x = chunkPos.getStartX() + localX;
        int z = chunkPos.getStartZ() + localZ;
        if (!world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return 0;
        }
        int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        CandyRegionType region = surface > 1 ? CandyRegionType.forSurfaceChunk(chunkPos)
                : CandyRegionType.forUndergroundChunk(chunkPos);
        int changed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int y = surface; y > Math.max(world.getBottomY(), surface - 14); y--) {
            pos.set(x, y, z);
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (state.isAir() || InfectionConversions.isInfected(state)) {
                continue;
            }
            InfectionConversions.Conversion conversion = InfectionConversions.get(block);
            if (conversion == null) {
                continue;
            }
            BlockState candy = conversion.toCandy().apply(state);
            if (candy == null) {
                continue;
            }
            // Deep layers keep their stone flavour; only the crust gets decorated.
            if (y < surface - 6 && !candy.isOf(CandyBlocks.CANDY_STONE) && region != CandyRegionType.DEEP_CANDY_CAVERNS) {
                candy = CandyBlocks.CANDY_STONE.getDefaultState();
            }
            if (region == CandyRegionType.DEEP_CANDY_CAVERNS && y > 20) {
                continue;
            }
            if (world.setBlockState(pos, candy, Block.NOTIFY_LISTENERS)) {
                stages.addChunkCount(pos, 1);
                stages.blockInfected();
                changed++;
                if (changed >= BUDGET_PER_TICK) {
                    break;
                }
            }
        }
        changed += decorate(world, x, z, surface, region, stages);
        return changed;
    }

    /** Region-specific decoration on top of the converted crust. */
    private static int decorate(ServerWorld world, int x, int z, int surface, CandyRegionType region,
                                InfectionWorldState stages) {
        if (surface <= 1 || world.random.nextInt(100) > 14) {
            return 0;
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int changed = 0;
        switch (region) {
            case GUMMY_FOREST -> {
                if (world.random.nextInt(100) < 20) {
                    changed += growGummyTree(world, x, surface, z, stages);
                } else {
                    changed += place(world, pos.set(x, surface + 1, z),
                            CandyBlocks.randomVegetation(world.random), stages);
                }
            }
            case CHOCOLATE_WASTELAND -> {
                if (world.random.nextInt(100) < 45) {
                    changed += place(world, pos.set(x, surface + 1, z),
                            CandyBlocks.CHOCOLATE_GROWTH.getDefaultState(), stages);
                } else {
                    changed += place(world, pos.set(x, surface + 1, z),
                            CandyBlocks.CHOCOLATE_BLOB.getDefaultState(), stages);
                }
            }
            case SUGAR_CRYSTAL_FIELDS -> {
                int size = 1 + world.random.nextInt(3);
                for (int i = 0; i < size; i++) {
                    changed += place(world, pos.set(x + world.random.nextInt(3) - 1, surface + 1 + i,
                            z + world.random.nextInt(3) - 1),
                            CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                                    .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, i), stages);
                }
            }
            case CARAMEL_SWAMP -> changed += place(world, pos.set(x, surface + 1, z),
                    world.random.nextBoolean() ? CandyBlocks.CARAMEL_GROWTH.getDefaultState()
                            : CandyBlocks.STICKY_SYRUP.getDefaultState(), stages);
            case CANDY_PLAINS -> {
                if (world.random.nextInt(100) < 30) {
                    changed += place(world, pos.set(x, surface + 1, z),
                            InfectionConversions.randomLollipop(world.random).getDefaultState(), stages);
                } else {
                    changed += place(world, pos.set(x, surface + 1, z),
                            InfectionConversions.randomHardCandy(world.random).getDefaultState(), stages);
                }
            }
            case DEEP_CANDY_CAVERNS -> changed += place(world, pos.set(x, surface + 1, z),
                    CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                            .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, world.random.nextInt(3)), stages);
        }
        return changed;
    }

    /** A gummy tree: gummy log trunk, gummy leaves, hanging candy vines. */
    public static int growGummyTree(ServerWorld world, int x, int groundY, int z, InfectionWorldState stages) {
        int height = 5 + world.random.nextInt(4);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int changed = 0;
        for (int y = 1; y <= height; y++) {
            changed += place(world, pos.set(x, groundY + y, z), CandyBlocks.GUMMY_LOG.getDefaultState()
                    .with(net.minecraft.block.PillarBlock.AXIS, net.minecraft.util.math.Direction.Axis.Y), stages);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = height - 2; dy <= height + 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy > height - 1) {
                        continue;
                    }
                    changed += place(world, pos.set(x + dx, groundY + dy, z + dz),
                            CandyBlocks.GUMMY_LEAVES.getDefaultState(), stages);
                }
            }
        }
        if (world.random.nextBoolean()) {
            for (int y = height - 2; y > height - 5; y--) {
                changed += place(world, pos.set(x, groundY + y, z + 2),
                        CandyBlocks.CANDY_VINES.getDefaultState(), stages);
            }
        }
        return changed;
    }

    private static int place(ServerWorld world, BlockPos pos, BlockState state, InfectionWorldState stages) {
        if (state == null || !world.getBlockState(pos).isReplaceable()) {
            return 0;
        }
        if (!world.setBlockState(pos, state, Block.NOTIFY_LISTENERS)) {
            return 0;
        }
        stages.addChunkCount(pos, 1);
        stages.blockInfected();
        return 1;
    }
}
