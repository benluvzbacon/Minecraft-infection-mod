package dev.candyinfection.client.particle;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/** Builds {@link CandyParticle} instances with a fixed colour per particle type. */
public class CandyParticleFactory implements ParticleFactory<SimpleParticleType> {
    private final FabricSpriteProvider sprites;
    private final float red;
    private final float green;
    private final float blue;
    private final float scale;
    private final int maxAge;
    private final float gravity;

    public CandyParticleFactory(FabricSpriteProvider sprites, float red, float green, float blue,
                                float scale, int maxAge, float gravity) {
        this.sprites = sprites;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.scale = scale;
        this.maxAge = maxAge;
        this.gravity = gravity;
    }

    @Override
    public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ) {
        CandyParticle particle = new CandyParticle(world, x, y, z, velocityX, velocityY, velocityZ,
                this.red, this.green, this.blue, this.scale, this.maxAge, this.gravity);
        particle.setSprite(this.sprites);
        return particle;
    }
}
