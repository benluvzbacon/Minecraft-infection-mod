package dev.candyinfection.entity;

import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyParticles;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Jawbreaker: a rolling ball of hard candy. It builds up speed in a straight
 * line, bounces off terrain, hits hard enough to throw the player backwards,
 * smashes fragile blocks and sometimes splits into two smaller jawbreakers when
 * it is destroyed.
 */
public class JawbreakerEntity extends CandyHostileEntity {
    private int rollTicks;
    private Vec3d rollDirection = Vec3d.ZERO;

    public JawbreakerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createJawbreakerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30.0D);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.rollTicks = 0;
            return;
        }
        this.rollTicks++;
        if (this.rollTicks % 30 == 1) {
            this.rollDirection = new Vec3d(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ()).normalize();
        }
        if (this.rollDirection.lengthSquared() > 0.001D && this.isOnGround()) {
            double speed = Math.min(1.35D, 0.4D + this.rollTicks * 0.01D);
            this.setVelocity(this.rollDirection.x * speed, this.getVelocity().y, this.rollDirection.z * speed);
            this.velocityModified = true;
            this.setYaw((float) (Math.atan2(this.rollDirection.z, this.rollDirection.x) * 180.0D / Math.PI) + 90.0F);
        }
        this.breakFragileBlocks();
    }

    private void breakFragileBlocks() {
        if (this.age % 6 != 0 || !(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        for (BlockPos candidate : BlockPos.iterate(pos.add(-1, 0, -1), pos.add(1, 1, 1))) {
            BlockState state = world.getBlockState(candidate);
            if (state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.FERN) || state.isOf(Blocks.TALL_GRASS)
                    || state.isOf(Blocks.GLASS) || state.isOf(Blocks.GLASS_PANE)) {
                world.breakBlock(candidate, true, this);
            }
        }
        world.spawnParticles(CandyParticles.CANDY_DUST, this.getX(), this.getY() + 0.3D, this.getZ(),
                3, 0.3D, 0.2D, 0.3D, 0.02D);
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target) {
        target.takeKnockback(1.8D, target.getX() - this.getX(), target.getZ() - this.getZ());
        this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 1.1F);
        // Bounce away after an impact so it can roll in again.
        this.setVelocity(this.getVelocity().multiply(-0.5D, 1.0D, -0.5D).add(0.0D, 0.35D, 0.0D));
        this.velocityModified = true;
        this.rollTicks = 0;
    }

    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);
        // Chance to split into smaller jawbreakers.
        if (this.getWorld().isClient || this.random.nextFloat() > 0.45F || this.getHealth() > 0.0F) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            var child = CandyEntities.GUMMY_SPAWN.create(world);
            if (child == null) {
                continue;
            }
            child.refreshPositionAndAngles(this.getX() + (i == 0 ? 0.6D : -0.6D), this.getY(), this.getZ(), this.getYaw(), 0.0F);
            world.spawnEntity(child);
        }
        this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.4F, 0.8F);
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, net.minecraft.entity.damage.DamageSource damageSource) {
        // Jawbreakers bounce instead of taking fall damage.
        if (fallDistance > 3.0F && !this.getWorld().isClient) {
            this.setVelocity(this.getVelocity().x, 0.32D, this.getVelocity().z);
            this.velocityModified = true;
            this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, 1.0F, 1.3F);
        }
        return false;
    }

    @Override
    public int candyColor() {
        return 0x9B5DE5;
    }
}
