package dev.candyinfection.infection;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.entity.CandyMob;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.random.Random;

/**
 * Drives the infection simulation once per server tick.
 *
 * <p>Responsibilities: drain the bounded spread queue, advance the infection
 * stage, tick purge zones, roll infection events and keep candy monster
 * populations under their configured caps.
 */
public final class InfectionRuntime {
    /** Event names accepted by {@code /candyinfection event <name>}. */
    public static final String[] EVENTS = {InfectionEvents.CANDY_BLOOM, InfectionEvents.SUGAR_STORM,
            InfectionEvents.INFECTION_SURGE, InfectionEvents.GUMMY_MIGRATION, InfectionEvents.CANDYFALL};

    private static final Map<RegistryKey<World>, ArrayDeque<BlockPos>> QUEUES = new HashMap<>();
    private static final Map<RegistryKey<World>, Integer> MONSTER_COUNTS = new HashMap<>();
    private static final Random RANDOM = Random.create();
    private static int tick;

    private InfectionRuntime() {
    }

    public static void register() {
        InfectionConversions.build();
        ServerTickEvents.END_WORLD_TICK.register(InfectionRuntime::onWorldTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            QUEUES.clear();
            MONSTER_COUNTS.clear();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                CandyLog.phase("Infection system online for " + server.getWorlds().iterator().hasNext()));
        CandyLog.phase("Infection spreading system initialised (budget driven, no chunk scanning)");
    }

    /** Adds a position to the spread queue, dropping it if the queue is full. */
    public static void enqueue(ServerWorld world, BlockPos pos) {
        CandyConfig config = CandyConfig.get();
        if (config.maxSpreadOperationsPerTick <= 0) {
            return;
        }
        ArrayDeque<BlockPos> queue = QUEUES.computeIfAbsent(world.getRegistryKey(), key -> new ArrayDeque<>());
        if (queue.size() >= config.maxSpreadQueueSize) {
            return;
        }
        queue.add(pos);
    }

    public static int queueSize(World world) {
        ArrayDeque<BlockPos> queue = QUEUES.get(world.getRegistryKey());
        return queue == null ? 0 : queue.size();
    }

    private static void onWorldTick(ServerWorld world) {
        tick++;
        CandyConfig config = CandyConfig.get();
        InfectionWorldState data = InfectionWorldState.get(world);

        // 1. spread budget ---------------------------------------------------
        ArrayDeque<BlockPos> queue = QUEUES.get(world.getRegistryKey());
        if (queue != null && !queue.isEmpty()) {
            int budget = config.maxSpreadOperationsPerTick;
            int surge = isSurgeActive(world) ? budget : 0;
            for (int i = 0; i < budget + surge && !queue.isEmpty(); i++) {
                BlockPos pos = queue.poll();
                if (pos == null) {
                    break;
                }
                InfectionSpread.spreadFrom(world, pos);
            }
        }

        // 1b. aggressive core-driven spread: even if queue is empty, cores keep pushing
        // This ensures infection starts even if random ticks are slow. Made MUCH faster per user request.
        if (tick % 5 == 0 && data.getCoreCount() > 0) {
            BlockPos core = data.nearestCore(world.getPlayers().isEmpty() ? BlockPos.ORIGIN : world.getPlayers().get(0).getBlockPos());
            if (core != null) {
                // Enqueue many positions near core to keep spread alive and fast
                for (int i = 0; i < 8; i++) {
                    BlockPos p = core.add(RANDOM.nextInt(21) - 10, RANDOM.nextInt(9) - 4, RANDOM.nextInt(21) - 10);
                    enqueue(world, p);
                }
                // Also directly convert nearby blocks every tick for visible fast spread
                if (tick % 20 == 0) {
                    for (int i = 0; i < 12; i++) {
                        BlockPos target = core.add(RANDOM.nextInt(17) - 8, RANDOM.nextInt(7) - 3, RANDOM.nextInt(17) - 8);
                        var state = world.getBlockState(target);
                        var conv = InfectionConversions.get(state.getBlock());
                        if (conv != null) {
                            InfectionSpread.convert(world, target, state, conv.toCandy().apply(state));
                        }
                    }
                }
            }
        }

        // 1c. natural shrine generation - rare candy shrines that spawn in overworld so players
        // can find infection without commands. This is the "structure that spawned so you can start
        // the infection yourself" that user requested.
        if (config.worldGenEnabled && config.shrineGenerationEnabled && tick % 200 == 0 && !world.getPlayers().isEmpty()) {
            if (RANDOM.nextFloat() < config.shrineSpawnChance * 2.0f) {
                try {
                    // Pick a random player and try to spawn shrine 100-300 blocks away
                    var player = world.getPlayers().get(RANDOM.nextInt(world.getPlayers().size()));
                    BlockPos playerPos = player.getBlockPos();
                    // Don't spawn too close to existing cores
                    BlockPos nearestCore = data.nearestCore(playerPos);
                    if (nearestCore == null || nearestCore.getSquaredDistance(playerPos) > 800 * 800) {
                        int angle = RANDOM.nextInt(360);
                        int dist = 150 + RANDOM.nextInt(200);
                        int x = playerPos.getX() + (int)(Math.cos(Math.toRadians(angle)) * dist);
                        int z = playerPos.getZ() + (int)(Math.sin(Math.toRadians(angle)) * dist);
                        BlockPos shrinePos = new BlockPos(x, 0, z);
                        // Only spawn if no core within 500 blocks of shrine pos
                        BlockPos nearCore = data.nearestCore(shrinePos);
                        if (nearCore == null || nearCore.getSquaredDistance(shrinePos) > 500 * 500) {
                            int built = dev.candyinfection.world.gen.CandyShrineStructure.build(world, shrinePos, data);
                            if (built > 20) {
                                CandyLog.init("Candy shrine generated at " + shrinePos.toShortString() + " (" + built + " blocks) - players can now find infection naturally");
                                // Announce to nearby players
                                for (var p : world.getPlayers()) {
                                    if (p.getBlockPos().getSquaredDistance(shrinePos) < 400 * 400) {
                                        p.sendMessage(net.minecraft.text.Text.translatable("message.candyinfection.shrine_found",
                                                shrinePos.getX(), shrinePos.getZ()).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE), false);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    CandyLog.debug("Shrine generation failed: " + e.getMessage());
                }
            }
        }

        // 2. purges ----------------------------------------------------------
        if (config.infectionDecayEnabled && !data.purges().isEmpty()) {
            for (InfectionWorldState.PurgeZone zone : data.purges()) {
                InfectionSpread.purgeTick(world, zone.center(), zone.radius(), 12);
            }
            data.tickPurges();
        }

        if (tick % 20 != 0) {
            return;
        }

        // 3. progression -----------------------------------------------------
        if (data.getTotalInfected() > 0) {
            data.addActiveTicks(20L);
        }
        data.tickEvent();
        if (config.difficultyScalingEnabled) {
            long ticksPerStage = (long) config.secondsPerStage * 20L;
            int targetStage = (int) Math.min(InfectionStages.MAX, 1L + data.getActiveTicks() / Math.max(1L, ticksPerStage));
            // Cores push the infection forward, destroyed cores hold it back.
            targetStage = Math.max(InfectionStages.MIN, targetStage);
            if (targetStage > data.getStage()) {
                data.setStage(targetStage);
                announce(world, "stage." + InfectionStages.of(targetStage).label().toLowerCase(java.util.Locale.ROOT));
            }
        }

        if (tick % 100 != 0) {
            return;
        }

        // 4. monster caps ----------------------------------------------------
        MONSTER_COUNTS.put(world.getRegistryKey(), countMonsters(world));

        // 5. region growth & structure building - now focuses on lollipops/candy per user request, NO gummy groves
        if (config.structuresEnabled && data.getTotalInfected() > 10 && RANDOM.nextInt(3) == 0) {
            try {
                BlockPos center = data.nearestCore(world.getPlayers().isEmpty() ? BlockPos.ORIGIN : world.getPlayers().get(0).getBlockPos());
                if (center == null && !world.getPlayers().isEmpty()) {
                    center = world.getPlayers().get(0).getBlockPos();
                }
                if (center != null) {
                    // Grow a random chunk near infection - more frequent now
                    for (int i = 0; i < 2; i++) {
                        BlockPos growPos = center.add(RANDOM.nextInt(41) - 20, 0, RANDOM.nextInt(41) - 20);
                        net.minecraft.util.math.ChunkPos chunkPos = new net.minecraft.util.math.ChunkPos(growPos);
                        if (world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                            dev.candyinfection.world.gen.CandyRegionGenerator.growChunk(world, chunkPos, RANDOM.nextInt(16), RANDOM.nextInt(16), data);
                        }
                    }
                    // Build candy structures - user wants lollipops and candy, NOT gummy groves
                    if (RANDOM.nextInt(4) == 0) {
                        String kind = switch (RANDOM.nextInt(4)) {
                            case 0 -> dev.candyinfection.world.gen.CandyStructures.LOLLIPOP_FIELD;
                            case 1 -> dev.candyinfection.world.gen.CandyStructures.HARD_CANDY_ARCH;
                            case 2 -> dev.candyinfection.world.gen.CandyStructures.SUGAR_SPIRE;
                            default -> dev.candyinfection.world.gen.CandyStructures.CHOCOLATE_MOUND;
                        };
                        BlockPos structPos = center.add(RANDOM.nextInt(51) - 25, 0, RANDOM.nextInt(51) - 25);
                        dev.candyinfection.world.gen.CandyStructures.build(world, structPos, kind, data);
                    }
                    // Extra lollipop clusters for user request
                    if (RANDOM.nextInt(3) == 0) {
                        for (int i = 0; i < 3; i++) {
                            BlockPos lollyPos = center.add(RANDOM.nextInt(31) - 15, 0, RANDOM.nextInt(31) - 15);
                            int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, lollyPos.getX(), lollyPos.getZ());
                            if (y > 1) {
                                BlockPos p = new BlockPos(lollyPos.getX(), y + 1, lollyPos.getZ());
                                if (world.isAir(p)) {
                                    world.setBlockState(p, dev.candyinfection.init.CandyBlocks.LOLLIPOP_PINK.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
                                    if (RANDOM.nextBoolean()) {
                                        world.setBlockState(p.up(), dev.candyinfection.init.CandyBlocks.LOLLIPOP_BLUE.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
                                    }
                                }
                            }
                        }
                    }
                    // Nests - more frequent for scarier infection
                    if (data.getNestCount() < 16 && RANDOM.nextInt(6) == 0) {
                        BlockPos nestPos = center.add(RANDOM.nextInt(35) - 17, 0, RANDOM.nextInt(35) - 17);
                        dev.candyinfection.world.gen.CandyStructures.build(world, nestPos, dev.candyinfection.world.gen.CandyStructures.INFECTION_NEST, data);
                    }
                }
            } catch (Exception e) {
                CandyLog.debug("Region/structure tick failed: " + e.getMessage());
            }
        }

        // 6. events ----------------------------------------------------------
        if (config.eventsEnabled && data.canStartEvent() && data.getTotalInfected() > 40 && RANDOM.nextInt(3) == 0) {
            InfectionEvents.roll(world, data);
        }
        if (isSurgeActive(world) || InfectionEvents.isBloomActive(world)) {
            InfectionEvents.tick(world, data);
        }

        // 7. boss ------------------------------------------------------------
        if (config.bossSpawningEnabled && data.getStage() >= InfectionStages.MAX && !data.isBossSpawned()
                && RANDOM.nextInt(20) == 0) {
            BlockPos core = data.nearestCore(BlockPos.ORIGIN);
            if (core != null) {
                InfectionEvents.summonBoss(world, core.up(3));
            }
        }
    }

    private static void announce(ServerWorld world, String key) {
        world.getPlayers().forEach(player ->
                player.sendMessage(net.minecraft.text.Text.translatable("message.candyinfection." + key), true));
    }

    /** True while an Infection Surge is running in this dimension. */
    public static boolean isSurgeActive(ServerWorld world) {
        InfectionWorldState data = InfectionWorldState.read(world);
        return data != null && data.isEventActive("infection_surge");
    }

    /** Number of candy monsters currently alive in this dimension. */
    public static int countMonsters(ServerWorld world) {
        int count = 0;
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof CandyMob) {
                count++;
            }
        }
        return count;
    }

    /** Cached count, refreshed every five seconds. */
    public static int cachedMonsterCount(ServerWorld world) {
        return MONSTER_COUNTS.getOrDefault(world.getRegistryKey(), 0);
    }

    /**
     * Effective monster cap for this dimension: the configured cap scaled by the
     * stage and by whether an Infection Surge is running. Caps are what keep
     * large infected areas playable, so they are only ever scaled, never removed.
     */
    public static int maxCandyMonsters(ServerWorld world) {
        CandyConfig config = CandyConfig.get();
        InfectionWorldState data = InfectionWorldState.read(world);
        float multiplier = config.monsterSpawnMultiplier
                * (data == null ? 1.0F : data.stageInfo().spawnMultiplier())
                * (isSurgeActive(world) ? 1.5F : 1.0F);
        return Math.max(0, Math.round(config.maxCandyMonsters * multiplier));
    }

    /**
     * Spawns up to {@code count} monsters of a specific type around
     * {@code center}, respecting the configured cap.
     *
     * @return how many actually spawned
     */
    public static int spawnMonsters(ServerWorld world, BlockPos center, EntityType<?> type, int count) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (cachedMonsterCount(world) >= maxCandyMonsters(world)) {
                break;
            }
            Entity entity = type.create(world);
            if (entity == null) {
                break;
            }
            BlockPos pos = findSpawnPosition(world, center);
            if (pos == null) {
                break;
            }
            entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    RANDOM.nextFloat() * 360.0F, 0.0F);
            if (entity instanceof MobEntity mob) {
                mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.MOB_SUMMONED, null);
                mob.setPersistent();
            }
            if (world.spawnEntity(entity)) {
                spawned++;
                MONSTER_COUNTS.merge(world.getRegistryKey(), 1, Integer::sum);
            }
        }
        return spawned;
    }

    /** Forces the world stage; used by {@code /candyinfection stage <n>}. */
    public static void setStage(ServerWorld world, int stage) {
        InfectionWorldState.get(world).setStage(stage);
        announce(world, "stage." + InfectionStages.of(stage).label().toLowerCase(java.util.Locale.ROOT));
    }

    /** Starts an infection event immediately; used by {@code /candyinfection event}. */
    public static void forceEvent(ServerWorld world, String event) {
        InfectionWorldState data = InfectionWorldState.get(world);
        BlockPos center = world.getPlayers().isEmpty() ? BlockPos.ORIGIN
                : world.getPlayers().get(0).getBlockPos();
        InfectionEvents.start(world, data, event, center);
    }

    /**
     * Spawns a candy monster appropriate for the current infection stage.
     *
     * @return the spawned entity, or {@code null} when the cap was reached
     */
    public static Entity spawnMonster(ServerWorld world, BlockPos pos, boolean ignoreCaps) {
        CandyConfig config = CandyConfig.get();
        InfectionWorldState data = InfectionWorldState.get(world);
        if (!ignoreCaps && cachedMonsterCount(world) >= config.maxCandyMonsters) {
            return null;
        }
        EntityType<? extends MobEntity> type = pickMonsterType(data.getStage());
        MobEntity entity = type.create(world);
        if (entity == null) {
            return null;
        }
        BlockPos spawnPos = findSpawnPosition(world, pos);
        if (spawnPos == null) {
            return null;
        }
        entity.refreshPositionAndAngles(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                RANDOM.nextFloat() * 360.0F, 0.0F);
        entity.initialize(world, world.getLocalDifficulty(spawnPos), SpawnReason.MOB_SUMMONED, null);
        if (world.spawnEntity(entity)) {
            MONSTER_COUNTS.merge(world.getRegistryKey(), 1, Integer::sum);
            return entity;
        }
        return null;
    }

    private static EntityType<? extends MobEntity> pickMonsterType(int stage) {
        int roll = RANDOM.nextInt(100);
        if (stage <= 2) {
            return roll < 70 ? CandyEntities.CANDY_CRAWLER : CandyEntities.SUGAR_LEECH;
        }
        if (stage <= 4) {
            if (roll < 40) {
                return CandyEntities.CANDY_CRAWLER;
            }
            if (roll < 60) {
                return CandyEntities.SUGAR_LEECH;
            }
            if (roll < 75) {
                return CandyEntities.CHOCOLATE_CREEPER;
            }
            if (roll < 90) {
                return CandyEntities.JAWBREAKER;
            }
            return CandyEntities.CANDY_MIMIC;
        }
        if (roll < 22) {
            return CandyEntities.CANDY_CRAWLER;
        }
        if (roll < 38) {
            return CandyEntities.JAWBREAKER;
        }
        if (roll < 52) {
            return CandyEntities.CANDY_MIMIC;
        }
        if (roll < 66) {
            return CandyEntities.CHOCOLATE_CREEPER;
        }
        if (roll < 78) {
            return CandyEntities.CARAMEL_BEAST;
        }
        if (roll < 90) {
            return CandyEntities.GUMMY_BRUTE;
        }
        if (roll < 97) {
            return CandyEntities.SUGAR_LEECH;
        }
        return CandyEntities.LOLLIPOP_STALKER;
    }

    private static BlockPos findSpawnPosition(ServerWorld world, BlockPos origin) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = origin.add(RANDOM.nextInt(17) - 8, RANDOM.nextInt(9) - 4, RANDOM.nextInt(17) - 8);
            if (world.isAir(pos) && world.isAir(pos.up())
                    && !world.getBlockState(pos.down()).isAir()
                    && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                return pos;
            }
        }
        return world.isAir(origin) ? origin : null;
    }
}
