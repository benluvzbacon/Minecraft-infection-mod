package dev.candyinfection.entity;

import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionRuntime;
import net.minecraft.block.BlockState;
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
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Candy Crawler: a mutated gummy insect.
 *
 * <p>Fast, climbs any surface (it extends the vanilla spider so wall climbing is
 * real), moves noticeably faster while standing on infected ground, spreads the
 * infection wherever it attacks, and splits off smaller gummy spawn creatures.
 */
public class CandyCrawlerEntity extends SpiderEntity implements CandyMob {
    private int gummyCooldown = 200;

    public CandyCrawlerEntity(EntityType<? extends SpiderEntity> entityType, World world) {
        super(entityType, world);
        this.gummyCooldown = 200 + this.random.nextInt(200);
    }

    public static DefaultAttributeContainer.Builder createCrawlerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.42D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData) {
        this.setPersistent();
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        // Faster on infected terrain.
        BlockState below = this.getWorld().getBlockState(this.getBlockPos().down());
        if (InfectionConversions.isInfected(below) && this.age % 20 == 0) {
            this.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.SPEED, 40, 0, false, false));
        }
        if (this.age % 8 == 0 && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.spawnParticles(CandyParticles.GUMMY_DROPLET, this.getX(), this.getY() + 0.2D, this.getZ(),
                    1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
        if (--this.gummyCooldown <= 0) {
            this.gummyCooldown = 400 + this.random.nextInt(400);
            this.spawnGummy();
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            this.applyInfectionOnHit(target instanceof LivingEntity living ? living : null);
            if (target instanceof LivingEntity living) {
                this.spreadAtTarget(living);
            }
        }
        return hit;
    }

    @Override
    public void applyInfectionOnHit(LivingEntity target) {
        if (target == null) {
            return;
        }
        CandyMob.super.applyInfectionOnHit(target);
    }

    /** Converts the ground under the victim into infected soil. */
    private void spreadAtTarget(LivingEntity target) {
        if (!(this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        BlockPos below = target.getBlockPos().down();
        BlockState state = serverWorld.getBlockState(below);
        InfectionConversions.Conversion conversion = InfectionConversions.get(state.getBlock());
        if (conversion != null) {
            dev.candyinfection.infection.InfectionSpread.convert(serverWorld, below, state, conversion.toCandy().apply(state));
        }
    }

    /** Spawns a smaller gummy creature, capped by the runtime. */
    public boolean spawnGummy() {
        if (!(this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) || !this.isAlive()) {
            return false;
        }
        if (InfectionRuntime.cachedMonsterCount(serverWorld) >= dev.candyinfection.config.CandyConfig.get().maxGummySpawns) {
            return false;
        }
        var gummy = CandyEntities.GUMMY_SPAWN.create(serverWorld);
        if (gummy == null) {
            return false;
        }
        gummy.refreshPositionAndAngles(this.getX() + 0.5D, this.getY(), this.getZ() + 0.5D, this.getYaw(), 0.0F);
        if (serverWorld.spawnEntity(gummy)) {
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 0.7F, 1.3F);
            return true;
        }
        return false;
    }

    @Override
    public float infectionPerHit() {
        return dev.candyinfection.config.CandyConfig.get().infectionPerAttack;
    }

    @Override
    public int candyColor() {
        return 0xFF3D94;
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.playSound(SoundEvents.ENTITY_SLIME_HURT, this.getSoundVolume(), 1.25F);
    }
}
