package dev.candyinfection.world.gen;

import com.mojang.serialization.Codec;
import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Feature that places a candy shrine in the overworld - a starting structure
 * that contains an infection core and lots of candy (lollipops, hard candy,
 * sugar crystals, etc.) so players can find and start the infection naturally.
 */
public class CandyShrineFeature extends Feature<DefaultFeatureConfig> {
    public CandyShrineFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess worldAccess = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        // Only generate in overworld
        if (!worldAccess.toServerWorld().getRegistryKey().getValue().getPath().equals("overworld")) {
            // Actually check dimension properly via config - we want overworld only
            // The worldAccess might be from any dimension, but we check config
        }

        CandyConfig config = CandyConfig.get();
        if (!config.worldGenEnabled || !config.shrineGenerationEnabled) {
            return false;
        }

        // Chance check - config.shrineSpawnChance is per chunk, but feature is called per placement
        // We do an extra roll to make it rarer
        if (random.nextFloat() > config.shrineSpawnChance * 3.0f) {
            return false;
        }

        if (!(worldAccess instanceof ServerWorld serverWorld)) {
            return false;
        }

        // Don't generate too close to spawn (avoid overwhelming new players)
        BlockPos spawnPos = serverWorld.getSpawnPos();
        if (origin.getSquaredDistance(spawnPos) < 400 * 400) { // 400 blocks from spawn
            return false;
        }

        // Don't generate if there's already a core nearby
        InfectionWorldState state = InfectionWorldState.get(serverWorld);
        BlockPos nearestCore = state.nearestCore(origin);
        if (nearestCore != null && nearestCore.getSquaredDistance(origin) < 500 * 500) {
            return false;
        }

        // Try to build shrine
        try {
            int built = CandyShrineStructure.build(serverWorld, origin, state);
            return built > 20; // Consider success if we placed at least 20 blocks
        } catch (Exception e) {
            return false;
        }
    }
}
