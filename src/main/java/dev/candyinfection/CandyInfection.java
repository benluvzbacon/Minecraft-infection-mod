package dev.candyinfection;

import dev.candyinfection.command.CandyInfectionCommand;
import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyBlockEntities;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyFeatures;
import dev.candyinfection.init.CandyItemGroups;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.init.CandyTags;
import dev.candyinfection.infection.InfectionRuntime;
import dev.candyinfection.infection.InfectedMobHandler;
import dev.candyinfection.infection.PlayerInfection;
import dev.candyinfection.network.CandyPayloads;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

/**
 * Common (server + client) entry point of the Candy Infection mod.
 *
 * <p>Everything that touches the game registry, the infection simulation or the
 * dedicated-server side of networking lives here. Client-only work (renderers,
 * HUD, particle factories) lives in the {@code client} source set so that a
 * dedicated server never loads a class that references rendering code.
 */
public final class CandyInfection implements ModInitializer {
    public static final String MOD_ID = "candyinfection";
    public static final String MOD_NAME = "Candy Infection";

    private CandyInfection() {
    }

    /** Builds a namespaced identifier for this mod. */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CandyLog.init("Loading " + MOD_NAME);

        CandyConfig.load();
        CandyLog.phase("Configuration loaded (spread speed " + CandyConfig.get().spreadSpeedMultiplier
                + ", player infection " + CandyConfig.get().playerInfectionEnabled + ")");

        CandyBlocks.register();
        CandyLog.phase("Registered " + CandyBlocks.count() + " blocks");

        CandyBlockEntities.register();
        CandyItems.register();
        CandyLog.phase("Registered " + CandyItems.count() + " items");

        CandyEffects.register();
        CandyParticles.register();
        CandyTags.register();
        CandyFeatures.register();
        CandyEntities.register();
        CandyLog.phase("Registered " + CandyEntities.count() + " entity types");

        CandyPayloads.register();
        CandyItemGroups.register();

        PlayerInfection.register();
        InfectedMobHandler.register();
        InfectionRuntime.register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> CandyInfectionCommand.register(dispatcher));

        CandyLog.init(MOD_NAME + " ready - the candy is spreading");
    }
}
