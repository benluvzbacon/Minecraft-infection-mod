package dev.candyinfection.client.particle;

import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.world.ClientWorld;

/**
 * A tinted billboard particle. All seven candy particles use this one class and
 * differ only in colour, drift and gravity, which keeps the particle system
 * cheap while still giving each effect a distinct look.
 */
public class CandyParticle extends SpriteBillboardParticle {
    CandyParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY,
                  double velocityZ, float red, float green, float blue, float scale, int maxAge, float gravity) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setColor(red, green, blue);
        this.scale(scale);
        this.setMaxAge(maxAge);
        this.setGravityStrength(gravity);
        this.collidesWithWorld = true;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        this.velocityY -= 0.04D * this.gravityStrength;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= 0.98D;
        this.velocityY *= 0.98D;
        this.velocityZ *= 0.98D;
    }
}
