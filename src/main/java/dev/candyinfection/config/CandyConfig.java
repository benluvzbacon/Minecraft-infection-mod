package dev.candyinfection.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON backed configuration, written to {@code config/candyinfection.json}.
 *
 * <p>The file is created with sensible defaults on first launch and is re-read
 * whenever {@code /candyinfection config reload} is used, so server owners can
 * tune the infection without restarting.
 */
public final class CandyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CandyConfig instance = new CandyConfig();

    /** Master switch for the spreading simulation. */
    public boolean spreadEnabled = true;

    // ---------------------------------------------------------------- spreading
    /** Multiplier applied to every infection spread chance. */
    public float spreadSpeedMultiplier = 1.0F;
    /** How many spread attempts the simulation may perform per server tick, per loaded dimension. */
    public int maxSpreadOperationsPerTick = 24;
    /** Maximum size of the pending spread queue before new entries are dropped. */
    public int maxSpreadQueueSize = 8192;
    /** Base chance (0..1) that an infected block tries to infect a neighbour on a random tick. */
    public float baseSpreadChance = 0.18F;
    /** Hard cap, in blocks, on how far a single infection front may travel from its seed. */
    public int maxInfectionRadius = 1024;
    /** Random tick speed applied to infected blocks (vanilla random tick rate is used when <= 0). */
    public int infectedBlockTickRate = 1;

    // ------------------------------------------------------------------- mobs
    /** Multiplier applied to candy monster spawn attempts. */
    public float monsterSpawnMultiplier = 1.0F;
    /** Whether candy monsters are added to the vanilla biome spawn tables. */
    public boolean monsterSpawningEnabled = true;
    /** Whether the infection may build candy structures and nests as it advances. */
    public boolean structuresEnabled = true;
    /** Per-dimension cap on simultaneously alive candy monsters. */
    public int maxCandyMonsters = 90;
    /** Per-dimension cap on the small gummy spawn creatures. */
    public int maxGummySpawns = 60;
    /** Whether vanilla mobs can be turned into infected variants. */
    public boolean infectVanillaMobs = true;
    /** Ticks a vanilla mob must stand on infected ground before it can turn (0 disables the timer). */
    public int vanillaMobInfectionTicks = 1800;
    /** Chance (0..1) per check that a vanilla mob standing on infected ground turns. */
    public float vanillaMobInfectionChance = 0.25F;
    /** Entity ids that are never infected. */
    public List<String> vanillaMobBlacklist = new ArrayList<>(List.of(
            "minecraft:ender_dragon", "minecraft:wither", "minecraft:warden"));

    // ----------------------------------------------------------------- player
    /** Whether players can be infected at all. */
    public boolean playerInfectionEnabled = true;
    /** Infection added per hostile candy attack. */
    public float infectionPerAttack = 4.0F;
    /** Infection added per second spent inside dense infected terrain. */
    public float infectionPerSecondInColony = 0.35F;
    /** Natural infection decay per second when the player is healthy and away from colonies. */
    public float naturalInfectionDecayPerSecond = 0.05F;
    /** Whether the on-screen infection meter is shown. */
    public boolean hudEnabled = true;

    // ------------------------------------------------------------------ world
    /** Whether candy world generation features (nests, crystal fields, ...) run. */
    public boolean worldGenEnabled = true;
    /** Extra logging for diagnosing spread behaviour. Never per-tick. */
    public boolean debugLogging = false;
    /** Multiplier for how often candy features are placed. */
    public float worldGenFrequency = 1.0F;
    /** Whether random infection events (Candy Bloom, Sugar Storm, ...) fire. */
    public boolean eventsEnabled = true;
    /** Whether The Confectioner boss may spawn naturally at infection stage 7. */
    public boolean bossSpawningEnabled = true;
    /** Whether the infection stage may advance over time (false freezes progression). */
    public boolean difficultyScalingEnabled = true;
    /** Seconds of active infection required to advance one infection stage. */
    public int secondsPerStage = 1500;
    /** Damage multiplier applied to candy monster attacks. */
    public float infectionDamageMultiplier = 1.0F;
    /** Whether infected blocks slowly decay back to their vanilla form when no core feeds them. */
    public boolean infectionDecayEnabled = true;

    /** Reloads the configuration from disk, creating it with defaults when absent. */
    public static void load() {
        Path file = configPath();
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                CandyConfig loaded = GSON.fromJson(json, CandyConfig.class);
                instance = loaded != null ? loaded : new CandyConfig();
                // Keep lists non-null even when the user deletes the section.
                if (instance.vanillaMobBlacklist == null) {
                    instance.vanillaMobBlacklist = new ArrayList<>();
                }
            } else {
                save();
            }
        } catch (Exception e) {
            CandyLog.warn("Could not read config, falling back to defaults: " + e.getMessage());
            instance = new CandyConfig();
            save();
        }
        instance.clamp();
        CandyLog.setDebug(instance.debugLogging);
    }

    /** Writes the current configuration to disk. */
    public static void save() {
        Path file = configPath();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CandyLog.warn("Could not write config: " + e.getMessage());
        }
    }

    public static CandyConfig get() {
        return instance;
    }

    /** Re-reads {@code config/candyinfection.json} without a restart. */
    public static void reload() {
        load();
        CandyLog.init("Configuration reloaded from " + configPath().getFileName()
                + " (spread " + instance.spreadSpeedMultiplier + "x, caps " + instance.maxCandyMonsters + ")");
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("candyinfection.json");
    }

    private void clamp() {
        this.spreadSpeedMultiplier = clamp(this.spreadSpeedMultiplier, 0.0F, 10.0F);
        this.maxSpreadOperationsPerTick = (int) clamp(this.maxSpreadOperationsPerTick, 0, 4096);
        this.maxSpreadQueueSize = (int) clamp(this.maxSpreadQueueSize, 64, 262144);
        this.baseSpreadChance = clamp(this.baseSpreadChance, 0.0F, 1.0F);
        this.maxInfectionRadius = (int) clamp(this.maxInfectionRadius, 16, 8192);
        this.monsterSpawnMultiplier = clamp(this.monsterSpawnMultiplier, 0.0F, 10.0F);
        this.maxCandyMonsters = (int) clamp(this.maxCandyMonsters, 0, 1000);
        this.maxGummySpawns = (int) clamp(this.maxGummySpawns, 0, 1000);
        this.vanillaMobInfectionChance = clamp(this.vanillaMobInfectionChance, 0.0F, 1.0F);
        this.infectionPerAttack = clamp(this.infectionPerAttack, 0.0F, 100.0F);
        this.infectionPerSecondInColony = clamp(this.infectionPerSecondInColony, 0.0F, 100.0F);
        this.naturalInfectionDecayPerSecond = clamp(this.naturalInfectionDecayPerSecond, 0.0F, 100.0F);
        this.worldGenFrequency = clamp(this.worldGenFrequency, 0.0F, 10.0F);
        this.infectionDamageMultiplier = clamp(this.infectionDamageMultiplier, 0.0F, 10.0F);
        this.secondsPerStage = (int) clamp(this.secondsPerStage, 60, 864000);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    /** True when the given entity id is allowed to be infected. */
    public boolean canInfect(String entityId) {
        return this.infectVanillaMobs && !this.vanillaMobBlacklist.contains(entityId);
    }
}
