package dev.candyinfection.infection;

import dev.candyinfection.init.CandyParticles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.function.Consumer;

/**
 * Candy explosions.
 *
 * <p>Implemented directly rather than through {@code World.createExplosion} so
 * the blast radius, damage falloff and block effects stay under the mod's
 * control: a Chocolate Creeper should melt the ground into candy, not punch
 * craters through a player's base.
 */
public final class CandyBlast {
    private CandyBlast() {
    }

    /**
     * Damages everything within {@code radius} of the centre with linear
     * falloff, then plays the candy blast effects.
     *
     * @param afterHit called for every entity the blast actually damaged
     */
    public static int explode(ServerWorld world, LivingEntity source, double x, double y, double z,
                              float radius, float damage, Consumer<LivingEntity> afterHit) {
        Box area = new Box(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        int hit = 0;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != source)) {
            double distance = Math.sqrt(target.squaredDistanceTo(x, y, z));
            if (distance > radius) {
                continue;
            }
            float falloff = (float) (1.0D - distance / radius);
            if (target.damage(world.getDamageSources().magic(), damage * Math.max(0.2F, falloff))) {
                hit++;
                if (afterHit != null) {
                    afterHit.accept(target);
                }
            }
        }
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2.0F, 0.9F);
        world.spawnParticles(CandyParticles.CHOCOLATE_FRAGMENT, x, y, z, 40, radius * 0.5D, radius * 0.4D,
                radius * 0.5D, 0.08D);
        world.spawnParticles(CandyParticles.INFECTION_SPARK, x, y, z, 30, radius * 0.4D, radius * 0.4D,
                radius * 0.4D, 0.03D);
        return hit;
    }

    /** Convenience overload that applies infection instead of a custom callback. */
    public static int explode(ServerWorld world, LivingEntity source, double x, double y, double z,
                              float radius, float damage, float infection) {
        return explode(world, source, x, y, z, radius, damage, target -> {
            if (target instanceof net.minecraft.entity.player.PlayerEntity player) {
                PlayerInfection.add(player, infection);
            } else {
                target.addStatusEffect(new StatusEffectInstance(
                        dev.candyinfection.init.CandyEffects.STICKY, 100, 0, false, true));
            }
        });
    }
}
