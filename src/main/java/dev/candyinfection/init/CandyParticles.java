package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** The seven custom particles the infection uses. */
public final class CandyParticles {
    /** Magenta sparks that drift up from infected ground. */
    public static final SimpleParticleType INFECTION_SPARK = register("infection_spark");
    /** Cyan sparks from purifiers and purification effects. */
    public static final SimpleParticleType PURIFICATION_SPARK = register("purification_spark");
    /** Soft pink dust that falls around candy plants. */
    public static final SimpleParticleType CANDY_DUST = register("candy_dust");
    /** Green gummy droplets shed by crawlers and gummy spawns. */
    public static final SimpleParticleType GUMMY_DROPLET = register("gummy_droplet");
    /** Brown shards thrown off chocolate explosions. */
    public static final SimpleParticleType CHOCOLATE_FRAGMENT = register("chocolate_fragment");
    /** Yellow sugar sparkle used for pickups and cures. */
    public static final SimpleParticleType SUGAR_SPARKLE = register("sugar_sparkle");
    /** Orange caramel bubbles rising from caramel blocks. */
    public static final SimpleParticleType CARAMEL_BUBBLE = register("caramel_bubble");

    private CandyParticles() {
    }

    private static SimpleParticleType register(String name) {
        return Registry.register(Registries.PARTICLE_TYPE, CandyInfection.id(name), FabricParticleTypes.simple(true));
    }

    public static void register() {
        dev.candyinfection.util.CandyLog.debug("Particles initialised");
    }

    /** All registered candy particle types, for the client renderer registration. */
    public static ParticleType<?>[] all() {
        return new ParticleType<?>[] {INFECTION_SPARK, PURIFICATION_SPARK, CANDY_DUST, GUMMY_DROPLET,
                CHOCOLATE_FRAGMENT, SUGAR_SPARKLE, CARAMEL_BUBBLE};
    }
}
