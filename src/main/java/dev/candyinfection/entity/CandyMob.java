package dev.candyinfection.entity;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Marker + behaviour shared by every candy creature.
 *
 * <p>Kept as an interface so that creatures with very different vanilla bases
 * (spiders, creepers, custom hostiles) can all participate in the infection
 * rules without a common superclass.
 */
public interface CandyMob {
    /** How much infection a single hit from this creature applies to a player. */
    default float infectionPerHit() {
        return CandyConfig.get().infectionPerAttack;
    }

    /** Colour used for particles and the HUD flavour text. */
    default int candyColor() {
        return 0xFF69B4;
    }

    /** Whether this creature counts towards the candy monster cap. */
    default boolean countsTowardsCap() {
        return true;
    }

    /** Applies the infection payload of a melee hit. */
    default void applyInfectionOnHit(LivingEntity target) {
        if (target instanceof PlayerEntity player) {
            PlayerInfection.add(player, infectionPerHit());
        }
    }
}
