import pathlib
import os

ROOT = pathlib.Path(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))) / "src/main/java/dev/candyinfection"


def load(rel):
    return (ROOT / rel).read_text()


def save(rel, text):
    (ROOT / rel).write_text(text)


# ---------------- CandyLog ----------------
s = load('util/CandyLog.java')
s = s.replace("""    public static final Logger LOGGER = LoggerFactory.getLogger("CandyInfection");

    private CandyLog() {
    }""",
"""    public static final Logger LOGGER = LoggerFactory.getLogger("CandyInfection");

    /**
     * Debug output is off by default and only ever emitted from coarse, low
     * frequency code paths, so the console is never spammed while large infected
     * areas tick. Flip it with {@code "debugLogging": true} in the config file.
     */
    private static boolean debugEnabled;

    private CandyLog() {
    }

    public static void setDebug(boolean enabled) {
        if (enabled != debugEnabled) {
            debugEnabled = enabled;
            LOGGER.info("[CandyInfection] Debug logging {}", enabled ? "enabled" : "disabled");
        }
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }""")
s = s.replace("""    public static void debug(String message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[CandyInfection] {}", message);
        }
    }""",
"""    public static void debug(String message) {
        if (debugEnabled) {
            LOGGER.info("[CandyInfection] [debug] {}", message);
        }
    }""")
save('util/CandyLog.java', s)

# ---------------- CandyConfig ----------------
s = load('config/CandyConfig.java')
s = s.replace("""        instance.clamp();
    }""",
"""        instance.clamp();
        CandyLog.setDebug(instance.debugLogging);
    }""")
save('config/CandyConfig.java', s)

# ---------------- InfectionRuntime ----------------
s = load('infection/InfectionRuntime.java')
s = s.replace("import java.util.Random;", "import net.minecraft.util.math.random.Random;")
s = s.replace("    private static final Random RANDOM = new Random();",
              "    private static final Random RANDOM = Random.create();")
s = s.replace("public final class InfectionRuntime {",
"""public final class InfectionRuntime {
    /** Event names accepted by {@code /candyinfection event <name>}. */
    public static final String[] EVENTS = {InfectionEvents.CANDY_BLOOM, InfectionEvents.SUGAR_STORM,
            InfectionEvents.INFECTION_SURGE, InfectionEvents.GUMMY_MIGRATION, InfectionEvents.CANDYFALL};
""", 1)
s = s.replace("""    /** Cached count, refreshed every five seconds. */
    public static int cachedMonsterCount(ServerWorld world) {
        return MONSTER_COUNTS.getOrDefault(world.getRegistryKey(), 0);
    }""",
"""    /** Cached count, refreshed every five seconds. */
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
        InfectionEvents.start(world, InfectionWorldState.get(world), event, null);
    }""")
save('infection/InfectionRuntime.java', s)

# ---------------- CandyEntities ----------------
s = load('init/CandyEntities.java')
s = s.replace("""    public static int count() {""",
"""    /** Registry names of every candy monster, for command completion. */
    public static List<String> monsterNames() {
        List<String> names = new java.util.ArrayList<>();
        for (EntityType<? extends MobEntity> type : monsters()) {
            names.add(net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).toString());
        }
        return names;
    }

    public static int count() {""")
save('init/CandyEntities.java', s)

# ---------------- command ----------------
s = load('command/CandyInfectionCommand.java')
s = s.replace("""        send(source, "Candy monsters alive (this dimension): " + CandyEntities.count(world)
                + " / cap " + InfectionRuntime.maxCandyMonsters(world));""",
"""        send(source, "Candy monsters alive (this dimension): " + InfectionRuntime.countMonsters(world)
                + " / cap " + InfectionRuntime.maxCandyMonsters(world));""")
s = s.replace("""        if (player != null) {
            PlayerInfection.InfectionLevel level = PlayerInfection.levelOf(player);
            send(source, "Your infection: " + String.format("%.1f%%", PlayerInfection.get(player))
                    + " (" + level.label() + ") - " + level.description());
        }""",
"""        if (player != null) {
            float level = PlayerInfection.get(player);
            send(source, "Your infection: " + String.format("%.1f%%", level)
                    + " (" + PlayerInfection.tierLabel(level) + ")");
        }""")
s = s.replace("""    /** Kept so the command file references the living-entity import path used elsewhere. */
    static boolean isLiving(LivingEntity entity) {
        return entity != null && entity.isAlive();
    }
""", "")
s = s.replace("import net.minecraft.entity.LivingEntity;\n", "")
save('command/CandyInfectionCommand.java', s)

print("CandyLog debug flag:", 'debugEnabled' in load('util/CandyLog.java'))
print("Runtime EVENTS:", 'public static final String[] EVENTS' in load('infection/InfectionRuntime.java'))
print("Runtime spawnMonsters:", 'public static int spawnMonsters' in load('infection/InfectionRuntime.java'))
print("Entities monsterNames:", 'monsterNames' in load('init/CandyEntities.java'))
print("Command levelOf gone:", 'levelOf' not in load('command/CandyInfectionCommand.java'))
