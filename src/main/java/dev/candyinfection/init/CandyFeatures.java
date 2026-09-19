package dev.candyinfection.init;

import dev.candyinfection.util.CandyLog;
import dev.candyinfection.world.gen.CandySpawns;

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
 * candy monsters also appear naturally at low weight.
 */
public final class CandyFeatures {
    private CandyFeatures() {
    }

    public static void register() {
        CandySpawns.register();
        CandyLog.debug("Candy world content wired (procedural regions + vanilla spawn tables)");
    }
}
