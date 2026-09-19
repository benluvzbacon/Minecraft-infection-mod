package dev.candyinfection.entity;

import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Caramel Beast: a slow, heavily armoured monster coated in hardened caramel.
 * It radiates a slowing aura, lobs sticky caramel projectiles and drops
 * caramel pools where it stands. It is also fireproof.
 */
public class CaramelBeastEntity extends CandyHostileEntity implements RangedAttackMob {
    public CaramelBeastEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createCaramelAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 14.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 26.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ProjectileAttackGoal(this, 0.9D, 60, 14.0F));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 0.9D, true));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        var projectile = CandyEntities.CANDY_PROJECTILE.create(world);
        if (projectile == null) {
            return;
        }
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.3333333333333333D) - projectile.getY();
        double dz = target.getZ() - this.getZ();
        double arc = Math.sqrt(dx * dx + dz * dz) * 0.20000000298023224D;
        projectile.setOwner(this);
        projectile.setVelocity(dx, dy + arc, dz, 1.2F, 3.0F);
        world.spawnEntity(projectile);
        this.playSound(SoundEvents.ENTITY_SLIME_ATTACK, 1.0F, 0.7F);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        // Slowing aura.
        if (this.age % 40 == 0) {
            for (PlayerEntity player : world.getPlayers()) {
                if (player.squaredDistanceTo(this) <= 64.0D) {
                    player.addStatusEffect(new StatusEffectInstance(CandyEffects.STICKY, 100, 0, false, true));
                }
            }
        }
        // Caramel pools where it stands.
        if (this.age % 120 == 0) {
            BlockPos below = this.getBlockPos().down();
            BlockState state = world.getBlockState(below);
            if (!state.isOf(CandyBlocks.CARAMEL_GROWTH) && !state.isAir()) {
                InfectionSpread.convert(world, below, state, CandyBlocks.CARAMEL_GROWTH.getDefaultState());
            }
        }
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public int candyColor() {
        return 0xB5651D;
    }
}
