package dev.candyinfection.client.particle;

import dev.candyinfection.init.CandyParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/**
 * Registers the seven custom particles.
 *
 * <p>Each one gets a sprite list from
 * {@code assets/candyinfection/particles/<name>.json}. If those files are missing
 * the client logs a warning and skips the sprite, which is the same graceful
 * fallback vanilla uses for a broken particle definition - the game keeps
 * running, it just does not draw that particle.
 */
public final class CandyParticleFactories {
    private CandyParticleFactories() {
    }

    public static void register() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        // magenta sparks, drift upward
        registry.register(CandyParticles.INFECTION_SPARK,
                sprites -> new CandyParticleFactory(sprites, 1.00F, 0.24F, 0.58F, 0.9F, 28, -0.02F));
        // cyan sparks, drift upward
        registry.register(CandyParticles.PURIFICATION_SPARK,
                sprites -> new CandyParticleFactory(sprites, 0.30F, 0.95F, 1.00F, 0.9F, 24, -0.03F));
        // soft pink dust, falls
        registry.register(CandyParticles.CANDY_DUST,
                sprites -> new CandyParticleFactory(sprites, 1.00F, 0.70F, 0.85F, 0.6F, 40, 0.02F));
        // green gummy droplet, falls and bounces
        registry.register(CandyParticles.GUMMY_DROPLET,
                sprites -> new CandyParticleFactory(sprites, 0.55F, 1.00F, 0.31F, 0.7F, 32, 0.06F));
        // brown chocolate shard
        registry.register(CandyParticles.CHOCOLATE_FRAGMENT,
                sprites -> new CandyParticleFactory(sprites, 0.42F, 0.24F, 0.12F, 0.8F, 22, 0.08F));
        // yellow sugar sparkle
        registry.register(CandyParticles.SUGAR_SPARKLE,
                sprites -> new CandyParticleFactory(sprites, 1.00F, 0.93F, 0.35F, 0.5F, 26, -0.01F));
        // orange caramel bubble, rises
        registry.register(CandyParticles.CARAMEL_BUBBLE,
                sprites -> new CandyParticleFactory(sprites, 1.00F, 0.55F, 0.10F, 0.7F, 30, -0.04F));
    }
}
