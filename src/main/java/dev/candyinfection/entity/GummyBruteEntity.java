package dev.candyinfection.entity;

import dev.candyinfection.entity.goal.ChargeAttackGoal;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Gummy Brute: the heavy hitter. Huge health, near-total knockback resistance,
 * a telegraphed charge that smashes through soft terrain, and it leaves infected
 * ground behind everywhere it walks.
 */
public class GummyBruteEntity extends CandyHostileEntity {
    public GummyBruteEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createBruteAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(2, new ChargeAttackGoal(this, 1.9D, 8.0F));
    }

    @Override
    protected double attackSpeed() {
        return 0.8D;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient || this.age % 20 != 0) {
            return;
        }
        // Leaves infected blocks behind as it moves.
        ServerWorld world = (ServerWorld) this.getWorld();
        BlockPos below = this.getBlockPos().down();
        BlockState state = world.getBlockState(below);
        InfectionConversions.Conversion conversion = InfectionConversions.get(state.getBlock());
        if (conversion != null) {
            InfectionSpread.convert(world, below, state, conversion.toCandy().apply(state));
        }
        if (this.age % 60 == 0) {
            world.spawnParticles(CandyParticles.GUMMY_DROPLET, this.getX(), this.getY() + 0.2D, this.getZ(),
                    6, 0.7D, 0.2D, 0.7D, 0.02D);
            this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 0.7F, 0.6F);
        }
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target) {
        target.takeKnockback(1.1D, this.getX() - target.getX(), this.getZ() - target.getZ());
    }

    @Override
    public int candyColor() {
        return 0xFF8C00;
    }
}
