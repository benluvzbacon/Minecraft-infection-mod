package dev.candyinfection.world.gen;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

/**
 * Adds the candy monsters to the vanilla spawn tables.
 *
 * <p>Every monster gets a low natural weight so they are rare until the infection
 * spreads; dense colonies produce them far faster through the runtime spawner,
 * which is where the real spawn pressure comes from.
 */
public final class CandySpawns {
    private CandySpawns() {
    }

    public static void register() {
        if (!CandyConfig.get().monsterSpawningEnabled) {
            CandyLog.init("Natural candy monster spawning is disabled in the config");
            return;
        }
        add(CandyEntities.CANDY_CRAWLER, 6, 1, 2);
        add(CandyEntities.GUMMY_SPAWN, 5, 2, 4);
        add(CandyEntities.SUGAR_LEECH, 4, 1, 2);
        add(CandyEntities.CANDY_MIMIC, 3, 1, 1);
        add(CandyEntities.JAWBREAKER, 3, 1, 1);
        add(CandyEntities.CHOCOLATE_CREEPER, 4, 1, 2);
        add(CandyEntities.LOLLIPOP_STALKER, 2, 1, 1);
        add(CandyEntities.GUMMY_BRUTE, 2, 1, 1);
        add(CandyEntities.CARAMEL_BEAST, 2, 1, 1);
        CandyLog.debug("Registered candy monster spawn entries");
    }

    private static void add(EntityType<? extends net.minecraft.entity.mob.MobEntity> type, int weight, int min, int max) {
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, type, weight, min, max);
    }
}
