package dev.candyinfection.infection;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.util.CandyLog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The spreading engine.
 *
 * <p>Nothing is ever scanned globally. An infected block only takes part when
 * it receives a random tick, at which point it may push its position onto a
 * bounded queue. {@link InfectionRuntime} drains at most
 * {@code maxSpreadOperationsPerTick} positions per tick per dimension, so the
 * cost of the infection is O(budget) instead of O(loaded blocks).
 */
public final class InfectionSpread {
    /** Offsets tried when an infected block spreads: biased horizontally. */
    private static final BlockPos[] HORIZONTAL = {
            BlockPos.ofFloored(1, 0, 0), BlockPos.ofFloored(-1, 0, 0),
            BlockPos.ofFloored(0, 0, 1), BlockPos.ofFloored(0, 0, -1)
    };
    private static final BlockPos[] VERTICAL = {
            BlockPos.ofFloored(0, 1, 0), BlockPos.ofFloored(0, -1, 0)
    };

    private static boolean converting;

    private InfectionSpread() {
    }

    /** Called from an infected block's random tick. - Made MUCH faster per user request */
    public static void onInfectedBlockTick(ServerWorld world, BlockPos pos, BlockState state, float multiplier) {
        CandyConfig config = CandyConfig.get();
        if (config.spreadSpeedMultiplier <= 0.0F) {
            return;
        }
        InfectionWorldState data = InfectionWorldState.get(world);
        float stageMultiplier = data.stageInfo().spreadMultiplier();
        // Increased base chance - user wants fast, scary spread
        float chance = config.baseSpreadChance * multiplier * stageMultiplier * config.spreadSpeedMultiplier * 1.8f;
        if (InfectionRuntime.isSurgeActive(world)) {
            chance *= 3.0F;
        }
        // Much higher chance to enqueue, and also enqueue neighbors directly for faster visible spread
        if (world.random.nextFloat() < Math.min(chance, 0.98F)) {
            InfectionRuntime.enqueue(world, pos.toImmutable());
            // 40% chance to also enqueue a random neighbor to accelerate spread
            if (world.random.nextInt(3) != 0) {
                BlockPos neighbor = pos.add(world.random.nextInt(3) - 1, world.random.nextInt(3) - 1, world.random.nextInt(3) - 1);
                InfectionRuntime.enqueue(world, neighbor);
            }
        }
        // Dense colonies also sprout vegetation directly - more frequent now
        if (world.random.nextInt(35) == 0 && world.isAir(pos.up())) {
            BlockState plant = InfectionConversions.randomVegetation(world.random, data.getStage());
            if (plant != null && plant.canPlaceAt(world, pos.up())) {
                world.setBlockState(pos.up(), plant, Block.NOTIFY_ALL);
            }
        }
        // Lollipops and other candy goodies now grow more often as infection spreads
        if (world.random.nextInt(60) == 0) {
            BlockPos up = pos.up();
            if (world.isAir(up)) {
                int roll = world.random.nextInt(100);
                BlockState candy = null;
                if (roll < 30) {
                    candy = InfectionConversions.randomLollipop(world.random).getDefaultState();
                } else if (roll < 55) {
                    candy = CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                            .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, world.random.nextInt(3));
                } else if (roll < 75) {
                    candy = CandyBlocks.HARD_CANDY_PINK.getDefaultState();
                    if (world.random.nextBoolean()) candy = InfectionConversions.randomHardCandy(world.random).getDefaultState();
                }
                if (candy != null && candy.canPlaceAt(world, up)) {
                    world.setBlockState(up, candy, Block.NOTIFY_ALL);
                }
            }
        }
    }

    /** Bookkeeping for a newly created infected block (worldgen or spread). */
    public static void onInfectedBlockPlaced(World world, BlockPos pos, BlockState state) {
        if (world.isClient || !InfectionConversions.isInfected(state)) {
            return;
        }
        if (world instanceof ServerWorld serverWorld) {
            InfectionWorldState data = InfectionWorldState.get(serverWorld);
            data.addChunkCount(pos, 1);
            data.blockInfected();
        }
    }

    /** Performs one spread operation for a queued position. - Made MUCH faster */
    public static boolean spreadFrom(ServerWorld world, BlockPos pos) {
        BlockState source = world.getBlockState(pos);
        if (!InfectionConversions.isInfected(source)) {
            return false;
        }
        InfectionWorldState data = InfectionWorldState.get(world);
        CandyConfig config = CandyConfig.get();
        Random random = world.random;
        float stageMultiplier = data.stageInfo().spreadMultiplier();
        // Much higher base chance for faster spread
        float baseChance = 0.85F * stageMultiplier * config.spreadSpeedMultiplier;
        if (InfectionRuntime.isSurgeActive(world)) {
            baseChance *= 2.5F;
        }

        boolean didSomething = false;
        // More attempts per operation - 4 normally, 8 during surge (was 2/4)
        int attempts = InfectionRuntime.isSurgeActive(world) ? 8 : 4;
        // At higher stages, even more attempts
        if (data.getStage() >= 4) attempts += 2;
        if (data.getStage() >= 6) attempts += 2;

        for (int i = 0; i < attempts; i++) {
            BlockPos[] pool = random.nextBoolean() ? HORIZONTAL : (random.nextInt(3) == 0 ? VERTICAL : HORIZONTAL);
            BlockPos offset = pool[random.nextInt(pool.length)];
            // Occasionally try further away for more aggressive spread
            if (random.nextInt(5) == 0) {
                offset = offset.add(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            }
            BlockPos target = pos.add(offset);
            BlockState targetState = world.getBlockState(target);
            if (targetState.isAir()) {
                didSomething |= growIntoAir(world, target, source, data);
                continue;
            }
            InfectionConversions.Conversion conversion = InfectionConversions.get(targetState.getBlock());
            if (conversion == null || conversion.minStage() > data.getStage()) {
                continue;
            }
            float chance = conversion.chance() * baseChance;
            if (random.nextFloat() < Math.min(chance, 0.99F)) {
                didSomething |= convert(world, target, targetState, conversion.toCandy().apply(targetState));
                // Chain reaction: if we converted, immediately try to spread from new block 30% of time
                if (didSomething && random.nextInt(3) == 0) {
                    InfectionRuntime.enqueue(world, target);
                }
            }
        }
        return didSomething;
    }

    /** Grows vines, crystals or growth into an air block next to the colony. */
    private static boolean growIntoAir(ServerWorld world, BlockPos target, BlockState source, InfectionWorldState data) {
        if (world.random.nextInt(6) != 0) {
            return false;
        }
        Block below = world.getBlockState(target.down()).getBlock();
        if (InfectionConversions.isInfected(below) && world.random.nextBoolean()) {
            BlockState plant = InfectionConversions.randomVegetation(world.random, data.getStage());
            if (plant != null && plant.canPlaceAt(world, target)) {
                return convert(world, target, world.getBlockState(target), plant);
            }
            return false;
        }
        if (source.isOf(CandyBlocks.GUMMY_LOG) || source.isOf(CandyBlocks.GUMMY_LEAVES)) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                if (world.isAir(target.offset(direction)) && world.random.nextInt(3) == 0) {
                    BlockPos vinePos = target.offset(direction);
                    return convert(world, vinePos, Blocks.AIR.getDefaultState(),
                            CandyBlocks.CANDY_VINES.getDefaultState().with(net.minecraft.block.HorizontalFacingBlock.FACING, direction.getOpposite()));
                }
            }
        }
        return false;
    }

    /** Replaces a block with its infected form and updates all bookkeeping. */
    public static boolean convert(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState) {
        if (converting) {
            return false;
        }
        if (!newState.canPlaceAt(world, pos)) {
            return false;
        }
        converting = true;
        try {
            boolean wasInfected = InfectionConversions.isInfected(oldState);
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            if (world.getBlockState(pos).isOf(newState.getBlock())) {
                InfectionWorldState data = InfectionWorldState.get(world);
                if (!wasInfected) {
                    data.addChunkCount(pos, 1);
                    data.blockInfected();
                    if (world.random.nextInt(12) == 0) {
                        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.35F, 1.2F + world.random.nextFloat() * 0.4F);
                    }
                    if (world.random.nextInt(4) == 0) {
                        world.spawnParticles(CandyParticles.INFECTION_SPARK, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                                4, 0.4D, 0.3D, 0.4D, 0.01D);
                    }
                }
                // Re-queue so the new block keeps spreading - now 70% chance instead of 33% for faster spread
                if (world.random.nextInt(10) < 7) {
                    InfectionRuntime.enqueue(world, pos.toImmutable());
                }
                return true;
            }
            return false;
        } finally {
            converting = false;
        }
    }

    /** Counts infected blocks in a small cube. Only used by commands/HUD. */
    public static int countInfectedNear(World world, BlockPos center, int radius) {
        int count = 0;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (InfectionConversions.isInfected(world.getBlockState(pos))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Reverts infected blocks in a purge zone. Called with a small budget so a
     * destroyed core never causes a single-tick spike.
     */
    public static int purgeTick(ServerWorld world, BlockPos center, int radius, int budget) {
        int reverted = 0;
        Random random = world.random;
        for (int i = 0; i < budget * 4 && reverted < budget; i++) {
            BlockPos pos = center.add(random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(radius * 2 + 1) - radius);
            BlockState state = world.getBlockState(pos);
            Block vanilla = InfectionConversions.vanillaForm(state.getBlock());
            if (vanilla == null) {
                continue;
            }
            converting = true;
            try {
                world.setBlockState(pos, vanilla.getDefaultState(), Block.NOTIFY_ALL);
            } finally {
                converting = false;
            }
            InfectionWorldState data = InfectionWorldState.get(world);
            data.addChunkCount(pos, -1);
            data.blockPurified();
            reverted++;
        }
        return reverted;
    }

    /** Debug helper used by {@code /candyinfection test}. */
    public static int createTestZone(ServerWorld world, BlockPos center, int radius) {
        InfectionConversions.build();
        int converted = InfectionConversions.infectArea(world, center, radius, 1.0F, InfectionStages.MAX, world.random);
        if (world.getBlockState(center).isAir() || !InfectionConversions.isInfected(world.getBlockState(center).getBlock())) {
            world.setBlockState(center, CandyBlocks.INFECTED_GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
        }
        CandyLog.debug("Created test infection zone at " + center.toShortString() + " (" + converted + " blocks)");
        return converted;
    }
}
