package dev.candyinfection.entity;

import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionWorldState;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Sticky candy projectile used by the Caramel Beast and The Confectioner.
 * On impact it damages, glues the target in place and seeds infection.
 */
public class CandyProjectileEntity extends ThrownEntity implements net.minecraft.entity.FlyingItemEntity {
    private float impactDamage = 5.0F;
    private float infectionAmount = 3.0F;
    private double impactRadius = 2.5D;

    public CandyProjectileEntity(EntityType<? extends ThrownEntity> entityType, World world) {
        super(entityType, world);
    }

    /** The projectile carries no synced state; its payload is set before spawning. */
    @Override
    protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
    }

    public CandyProjectileEntity(World world, LivingEntity owner) {
        super(CandyEntities.CANDY_PROJECTILE, owner, world);
    }

    /** The item the client renders while this projectile is in flight. */
    @Override
    public net.minecraft.item.ItemStack getStack() {
        return new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.SUGAR_SHARD);
    }

    /** Configures the payload of the projectile. */
    public CandyProjectileEntity configure(float damage, float infection, double radius) {
        this.impactDamage = damage;
        this.infectionAmount = infection;
        this.impactRadius = radius;
        return this;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!(this.getWorld() instanceof ServerWorld world)) {
            this.discard();
            return;
        }
        BlockPos pos = this.getBlockPos();
        world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.HOSTILE, 1.0F, 0.9F);
        world.spawnParticles(CandyParticles.CARAMEL_BUBBLE, this.getX(), this.getY(), this.getZ(),
                18, 0.5D, 0.4D, 0.5D, 0.03D);

        if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            living.damage(this.getDamageSources().thrown(this, this.getOwner()), this.impactDamage);
            living.addStatusEffect(new StatusEffectInstance(dev.candyinfection.init.CandyEffects.STICKY, 140, 1, false, true));
            if (living instanceof net.minecraft.entity.player.PlayerEntity player) {
                PlayerInfection.add(player, this.infectionAmount);
            }
        }

        // Seeds infection where it lands.
        int stage = InfectionWorldState.get(world).getStage();
        InfectionConversions.infectArea(world, pos, this.impactRadius, 0.6F, stage, this.random);
        this.discard();
    }

    /** Item shown when the projectile is picked up (it cannot actually be picked up). */
    public Item getRepresentedItem() {
        return CandyItems.GUMMY_RESIN;
    }
}
