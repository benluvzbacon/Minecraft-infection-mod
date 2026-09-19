package dev.candyinfection.entity;

import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Shared behaviour for every candy monster: hostile AI, a candy particle aura,
 * an infection payload on melee hits and a small boost while standing on
 * infected ground. Subclasses add their own special mechanics.
 */
public abstract class CandyHostileEntity extends HostileEntity implements CandyMob {
    protected CandyHostileEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(4, new MeleeAttackGoal(this, this.attackSpeed(), true));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, this.wanderSpeed()));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    protected double attackSpeed() {
        return 1.0D;
    }

    protected double wanderSpeed() {
        return 0.9D;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData) {
        this.setPersistent();
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }
        if (this.age % 14 == 0) {
            ServerWorld world = (ServerWorld) this.getWorld();
            world.spawnParticles(CandyParticles.CANDY_DUST, this.getX(), this.getY() + this.getHeight() * 0.7D, this.getZ(),
                    1, 0.3D, 0.35D, 0.3D, 0.005D);
        }
        if (this.age % 30 == 0 && InfectionConversions.isInfected(this.getWorld().getBlockState(this.getBlockPos().down()))) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, false, false));
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        float multiplier = CandyConfig.get().infectionDamageMultiplier;
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            this.applyInfectionOnHit(living);
            this.onSuccessfulHit(living);
        }
        return hit;
    }

    /** Hook for special on-hit behaviour. */
    protected void onSuccessfulHit(LivingEntity target) {
    }

    protected float damageMultiplier() {
        return CandyConfig.get().infectionDamageMultiplier;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SLIME_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SLIME_DEATH;
    }

    @Override
    public float infectionPerHit() {
        return CandyConfig.get().infectionPerAttack;
    }
}
