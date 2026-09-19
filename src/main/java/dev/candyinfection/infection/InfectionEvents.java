package dev.candyinfection.infection;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.util.CandyLog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/**
 * Random, world-level infection events. Each one is short lived and local so it
 * reads as a scare rather than a punishment, and every one of them can be
 * disabled in the config.
 */
public final class InfectionEvents {
    public static final String CANDY_BLOOM = "candy_bloom";
    public static final String SUGAR_STORM = "sugar_storm";
    public static final String INFECTION_SURGE = "infection_surge";
    public static final String GUMMY_MIGRATION = "gummy_migration";
    public static final String CANDYFALL = "candyfall";

    private static final Random RANDOM = new Random();

    private InfectionEvents() {
    }

    /** Picks and starts a random event near a player that is inside a colony. */
    public static void roll(ServerWorld world, InfectionWorldState data) {
        PlayerEntity player = pickPlayerInColony(world, data);
        if (player == null) {
            return;
        }
        String[] options = {CANDY_BLOOM, SUGAR_STORM, INFECTION_SURGE, GUMMY_MIGRATION, CANDYFALL};
        String event = options[RANDOM.nextInt(options.length)];
        start(world, data, event, player.getBlockPos());
    }

    /** Starts a specific event, used by both the scheduler and the debug command. */
    public static void start(ServerWorld world, InfectionWorldState data, String event, BlockPos center) {
        int ticks = switch (event) {
            case SUGAR_STORM -> 1800;
            case INFECTION_SURGE -> 1200;
            case GUMMY_MIGRATION -> 1500;
            default -> 600;
        };
        data.startEvent(event, ticks);
        world.playSound(null, center, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.AMBIENT, 1.6F, 0.7F);
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(Vec3d.ofCenter(center)) < 96.0D * 96.0D) {
                player.sendMessage(Text.translatable("event.candyinfection." + event), false);
            }
        }
        switch (event) {
            case CANDY_BLOOM -> bloom(world, center, 14);
            case CANDYFALL -> candyfall(world, center, 26);
            case GUMMY_MIGRATION -> migration(world, center, data);
            default -> {
            }
        }
        CandyLog.init("Infection event '" + event + "' at " + center.toShortString());
    }

    /** Per-tick behaviour while an event runs. */
    public static void tick(ServerWorld world, InfectionWorldState data) {
        String event = data.getActiveEvent();
        List<? extends PlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }
        switch (event) {
            case SUGAR_STORM -> {
                for (PlayerEntity player : players) {
                    BlockPos pos = player.getBlockPos();
                    world.spawnParticles(CandyParticles.SUGAR_SPARKLE, pos.getX() + 0.5D, pos.getY() + 8.0D, pos.getZ() + 0.5D,
                            14, 8.0D, 1.0D, 8.0D, 0.02D);
                    if (RANDOM.nextInt(30) == 0) {
                        InfectionRuntime.spawnMonster(world, pos, false);
                    }
                }
            }
            case INFECTION_SURGE -> {
                for (PlayerEntity player : players) {
                    if (RANDOM.nextInt(6) == 0) {
                        InfectionRuntime.enqueue(world, player.getBlockPos().add(
                                RANDOM.nextInt(33) - 16, RANDOM.nextInt(17) - 8, RANDOM.nextInt(33) - 16));
                    }
                }
            }
            case GUMMY_MIGRATION -> {
                if (RANDOM.nextInt(12) == 0) {
                    PlayerEntity player = players.get(RANDOM.nextInt(players.size()));
                    InfectionRuntime.spawnMonster(world, player.getBlockPos().add(
                            RANDOM.nextInt(41) - 20, 0, RANDOM.nextInt(41) - 20), false);
                }
            }
            default -> {
            }
        }
    }

    public static boolean isBloomActive(ServerWorld world) {
        InfectionWorldState data = InfectionWorldState.read(world);
        return data != null && data.getActiveEventTicks() > 0;
    }

    /** Rapidly grows the colony around {@code center}. */
    public static int bloom(ServerWorld world, BlockPos center, int radius) {
        int converted = InfectionConversions.infectArea(world, center, radius, 0.85F,
                InfectionWorldState.get(world).getStage(), RANDOM);
        world.spawnParticles(CandyParticles.INFECTION_SPARK, center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D,
                60, radius / 2.0D, 2.0D, radius / 2.0D, 0.05D);
        return converted;
    }

    /** Large candy growths burst out of the ground across an area. */
    public static int candyfall(ServerWorld world, BlockPos center, int attempts) {
        int placed = 0;
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = center.add(RANDOM.nextInt(41) - 20, RANDOM.nextInt(9) - 4, RANDOM.nextInt(41) - 20);
            if (!world.isAir(pos) || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                continue;
            }
            BlockState growth = InfectionConversions.randomVegetation(RANDOM, InfectionWorldState.get(world).getStage());
            if (growth != null && growth.canPlaceAt(world, pos)) {
                world.setBlockState(pos, growth, Block.NOTIFY_ALL);
                InfectionSpread.onInfectedBlockPlaced(world, pos, growth);
                placed++;
            }
        }
        return placed;
    }

    /** Sends a wave of candy monsters towards uninfected ground. */
    public static int migration(ServerWorld world, BlockPos center, InfectionWorldState data) {
        int spawned = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos pos = center.add(RANDOM.nextInt(33) - 16, 0, RANDOM.nextInt(33) - 16);
            Entity entity = InfectionRuntime.spawnMonster(world, pos, false);
            if (entity != null) {
                spawned++;
            }
        }
        return spawned;
    }

    /** Summons The Confectioner, the boss of the mod. */
    public static boolean summonBoss(ServerWorld world, BlockPos pos) {
        CandyConfig config = CandyConfig.get();
        if (!config.bossSpawningEnabled) {
            return false;
        }
        var boss = CandyEntities.CONFECTIONER.create(world);
        if (boss == null) {
            return false;
        }
        boss.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        if (world.spawnEntity(boss)) {
            InfectionWorldState.get(world).setBossSpawned(true);
            world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3.0F, 0.8F);
            for (PlayerEntity player : world.getPlayers()) {
                player.sendMessage(Text.translatable("message.candyinfection.boss_spawned"), false);
            }
            CandyLog.init("The Confectioner has awakened at " + pos.toShortString());
            return true;
        }
        return false;
    }

    private static PlayerEntity pickPlayerInColony(ServerWorld world, InfectionWorldState data) {
        for (PlayerEntity player : world.getPlayers()) {
            if (data.isColony(player.getBlockPos())) {
                return player;
            }
        }
        return null;
    }

    /** Helper for commands: reports which events exist. */
    public static String[] allEvents() {
        return new String[]{CANDY_BLOOM, SUGAR_STORM, INFECTION_SURGE, GUMMY_MIGRATION, CANDYFALL};
    }

    /** Used by the sugar storm to make infected terrain more aggressive. */
    public static void reinforce(ServerWorld world, BlockPos center) {
        Block below = world.getBlockState(center.down()).getBlock();
        if (below == CandyBlocks.INFECTED_GRASS_BLOCK || below == CandyBlocks.INFECTED_DIRT) {
            InfectionSpread.onInfectedBlockTick(world, center.down(), world.getBlockState(center.down()), 2.0F);
        }
    }

    /** Convenience for spawning a mob with a spawn reason recorded in its data. */
    public static Entity spawnAt(ServerWorld world, net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> type, BlockPos pos) {
        var mob = type.create(world);
        if (mob == null) {
            return null;
        }
        mob.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, RANDOM.nextFloat() * 360.0F, 0.0F);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.MOB_SUMMONED, null);
        return world.spawnEntity(mob) ? mob : null;
    }
}
