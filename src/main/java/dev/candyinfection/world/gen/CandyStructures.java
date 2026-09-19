package dev.candyinfection.world.gen;

import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Candy structures.
 *
 * <p>Structures are built procedurally by the infection runtime once a colony is
 * dense enough, rather than as worldgen templates. That keeps them inside loaded
 * chunks (no chunk-generation cost), makes them appear as the infection advances
 * rather than existing from the moment a chunk is created, and lets them be
 * purged cleanly when a core is destroyed.
 */
public final class CandyStructures {
    public static final String GUMMY_GROVE = "gummy_grove";
    public static final String HARD_CANDY_ARCH = "hard_candy_arch";
    public static final String CHOCOLATE_MOUND = "chocolate_mound";
    public static final String SUGAR_SPIRE = "sugar_spire";
    public static final String LOLLIPOP_FIELD = "lollipop_field";
    public static final String INFECTION_NEST = "infection_nest";

    private static final String[] KINDS = {GUMMY_GROVE, HARD_CANDY_ARCH, CHOCOLATE_MOUND, SUGAR_SPIRE, LOLLIPOP_FIELD};

    private CandyStructures() {
    }

    public static String[] kinds() {
        return KINDS;
    }

    /** Builds a structure of the given kind centred on {@code center}. */
    public static int build(ServerWorld world, BlockPos center, String kind, InfectionWorldState state) {
        int surface = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, center.getX(), center.getZ());
        if (surface <= 1) {
            return 0;
        }
        BlockPos ground = new BlockPos(center.getX(), surface, center.getZ());
        return switch (kind) {
            case GUMMY_GROVE -> buildGrove(world, ground, state);
            case HARD_CANDY_ARCH -> buildArch(world, ground, state);
            case CHOCOLATE_MOUND -> buildMound(world, ground, state);
            case SUGAR_SPIRE -> buildSpire(world, ground, state);
            case LOLLIPOP_FIELD -> buildLollipopField(world, ground, state);
            case INFECTION_NEST -> buildNest(world, ground, state);
            default -> 0;
        };
    }

    /** Builds a nest: a caramel-walled pocket that the runtime spawns from. */
    public static int buildNest(ServerWorld world, BlockPos ground, InfectionWorldState state) {
        int changed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    boolean shell = Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 0 || dy == 3;
                    pos.set(ground.getX() + dx, ground.getY() + dy, ground.getZ() + dz);
                    BlockState target = shell ? CandyBlocks.CARAMEL_GROWTH.getDefaultState() : null;
                    if (shell) {
                        changed += set(world, pos, target, state);
                    } else {
                        changed += clear(world, pos, state);
                    }
                }
            }
        }
        // Doorway so players can get in and monsters can get out.
        pos.set(ground.getX() - 2, ground.getY() + 1, ground.getZ());
        changed += clear(world, pos, state);
        pos.set(ground.getX() - 2, ground.getY() + 2, ground.getZ());
        changed += clear(world, pos, state);
        // The egg at the middle of the nest.
        changed += set(world, pos.set(ground.getX(), ground.getY() + 1, ground.getZ()),
                CandyBlocks.CHOCOLATE_BLOB.getDefaultState(), state);
        state.addNest(ground);
        return changed;
    }

    private static int buildGrove(ServerWorld world, BlockPos ground, InfectionWorldState state) {
        int changed = 0;
        for (int i = 0; i < 4; i++) {
            int dx = world.random.nextInt(9) - 4;
            int dz = world.random.nextInt(9) - 4;
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                    ground.getX() + dx, ground.getZ() + dz);
            if (y > 1) {
                changed += CandyRegionGenerator.growGummyTree(world, ground.getX() + dx, y, ground.getZ() + dz, state);
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (world.random.nextBoolean()) {
                    continue;
                }
                int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                        ground.getX() + dx, ground.getZ() + dz);
                if (y > 1) {
                    changed += set(world, new BlockPos.Mutable(ground.getX() + dx, y + 1, ground.getZ() + dz),
                            CandyBlocks.GUMMY_GROWTH.getDefaultState(), state);
                }
            }
        }
        return changed;
    }

    private static int buildArch(ServerWorld world, BlockPos ground, InfectionWorldState state) {
        int changed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockState candy = InfectionConversions.randomHardCandy(world.random).getDefaultState();
        Direction.Axis axis = world.random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        for (int i = -3; i <= 3; i++) {
            int x = axis == Direction.Axis.X ? ground.getX() + i : ground.getX();
            int z = axis == Direction.Axis.Z ? ground.getZ() + i : ground.getZ();
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z);
            if (y <= 1) {
                continue;
            }
            int height = 4 - Math.abs(i);
            for (int dy = 1; dy <= height; dy++) {
                changed += set(world, pos.set(x, y + dy, z), candy, state);
            }
        }
        return changed;
    }

    private static int buildMound(ServerWorld world, BlockPos ground, InfectionWorldState state) {
        int changed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int radius = Math.abs(dx) + Math.abs(dz);
                if (radius > 4) {
                    continue;
                }
                int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                        ground.getX() + dx, ground.getZ() + dz);
                if (y <= 1) {
                    continue;
                }
                for (int dy = 0; dy <= 3 - radius / 2; dy++) {
                    changed += set(world, pos.set(ground.getX() + dx, y + dy, ground.getZ() + dz),
                            CandyBlocks.CHOCOLATE_BLOB.getDefaultState(), state);
                }
                if (world.random.nextInt(100) < 25) {
                    changed += set(world, pos.set(ground.getX() + dx, y + 1, ground.getZ() + dz),
                            CandyBlocks.CHOCOLATE_GROWTH.getDefaultState(), state);
                }
            }
        }
        return changed;
    }

    private static int buildSpire(ServerWorld world, BlockPos ground, InfectionWorldState state) {
        int changed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int height = 7 + world.random.nextInt(5);
        for (int dy = 0; dy < height; dy++) {
            int radius = dy > height - 4 ? 0 : (dy > height - 7 ? 1 : 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockState block = (dx == 0 && dz == 0) ? CandyBlocks.SUGAR_CRYSTAL_BLOCK.getDefaultState()
                            : CandyBlocks.HARD_CANDY_CYAN.getDefaultState();
                    changed += set(world, pos.set(ground.getX() + dx, ground.getY() + dy, ground.getZ() + dz), block, state);
                }
            }
        }
        changed += set(world, pos.set(ground.getX(), ground.getY() + height, ground.getZ()),
                CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                        .with(net.minecraft.block.BlockProperties.TRIPLE_SIZE, 2), state);
        return changed;
    }

    private static int buildLollipopField(ServerWorld world, BlockPos ground, InfectionWorldState state) {
        int changed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (world.random.nextInt(100) < 55) {
                    continue;
                }
                int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                        ground.getX() + dx, ground.getZ() + dz);
                if (y <= 1) {
                    continue;
                }
                Block lollipop = InfectionConversions.randomLollipop(world.random);
                for (int dy = 1; dy <= 2 + world.random.nextInt(2); dy++) {
                    changed += set(world, pos.set(ground.getX() + dx, y + dy, ground.getZ() + dz),
                            lollipop.getDefaultState(), state);
                }
            }
        }
        return changed;
    }

    private static int set(ServerWorld world, BlockPos pos, BlockState state, InfectionWorldState record) {
        if (state == null || !world.getBlockState(pos).isReplaceable()) {
            return 0;
        }
        if (!world.setBlockState(pos, state, Block.NOTIFY_LISTENERS)) {
            return 0;
        }
        record.addChunkCount(pos, 1);
        record.blockInfected();
        return 1;
    }

    private static int clear(ServerWorld world, BlockPos pos, InfectionWorldState record) {
        if (world.getBlockState(pos).isAir()) {
            return 0;
        }
        if (!world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS)) {
            return 0;
        }
        record.addChunkCount(pos, -1);
        return 1;
    }
}
