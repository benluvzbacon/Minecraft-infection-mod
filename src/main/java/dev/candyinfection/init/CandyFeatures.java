package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.util.CandyLog;
import dev.candyinfection.world.gen.CandyShrineFeature;
import dev.candyinfection.world.gen.CandySpawns;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * World-content wiring.
 *
 * <p>Candy terrain is grown by the infection simulation itself rather than by
 * worldgen features: regions, structures and nests are only ever built inside
 * already-loaded chunks as the infection reaches them. That keeps existing
 * worlds byte-for-byte untouched, needs no datapack, and cannot corrupt chunks
 * that were generated before the mod was installed.
 *
 * <p>What this class does contribute is the vanilla spawn-table integration, so
 * candy monsters also appear naturally at low weight, plus the candy shrine
 * starting structure that spawns rarely in the overworld so players can find
 * the infection without commands.
 */
public final class CandyFeatures {
    public static Feature<DefaultFeatureConfig> CANDY_SHRINE_FEATURE;

    private CandyFeatures() {
    }

    public static void register() {
        // Register the shrine feature that generates the starting structure
        CANDY_SHRINE_FEATURE = Registry.register(Registries.FEATURE, CandyInfection.id("candy_shrine"),
                new CandyShrineFeature(DefaultFeatureConfig.CODEC));

        CandySpawns.register();
        CandyLog.debug("Candy world content wired (procedural regions + vanilla spawn tables + shrine feature)");

        // Shrine generation is handled via InfectionRuntime's chunk tick logic
        // plus optional feature placement - see CandyShrineFeature and InfectionRuntime
        if (CandyConfig.get().shrineGenerationEnabled) {
            CandyLog.phase("Candy shrines will generate rarely in overworld (chance " + CandyConfig.get().shrineSpawnChance + " per chunk)");
        }
    }
}
