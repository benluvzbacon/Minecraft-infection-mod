package dev.candyinfection.entity.goal;

import dev.candyinfection.init.CandyParticles;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * The Gummy Brute's charge: it winds up, then barrels straight at the player,
 * smashing soft blocks out of the way and dealing heavy impact damage.
 */
public class ChargeAttackGoal extends Goal {
    private final PathAwareEntity mob;
    private final double chargeSpeed;
    private final float impactDamage;
    private LivingEntity target;
    private int windup;
    private int charging;
    private int cooldown;

    public ChargeAttackGoal(PathAwareEntity mob, double chargeSpeed, float impactDamage) {
        this.mob = mob;
        this.chargeSpeed = chargeSpeed;
        this.impactDamage = impactDamage;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || this.cooldown > 0) {
            return false;
        }
        double distance = this.mob.squaredDistanceTo(target);
        return distance > 16.0D && distance < 400.0D && this.mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.target = this.mob.getTarget();
        this.windup = 20;
        this.charging = 0;
        this.mob.setAttacking(true);
    }

    @Override
    public void stop() {
        this.target = null;
        this.windup = 0;
        this.charging = 0;
        this.cooldown = 140;
        this.mob.setAttacking(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.target == null || !this.target.isAlive()) {
            return;
        }
        this.mob.getLookControl().lookAt(this.target, 30.0F, 30.0F);
        if (this.windup > 0) {
            this.windup--;
            this.mob.getMoveControl().strafeTo(-0.5F, 0.0F);
            return;
        }
        this.charging++;
        Vec3d direction = new Vec3d(this.target.getX() - this.mob.getX(), 0.0D, this.target.getZ() - this.mob.getZ()).normalize();
        this.mob.getMoveControl().moveTo(this.mob.getX() + direction.x * 3.0D, this.mob.getY(),
                this.mob.getZ() + direction.z * 3.0D, this.chargeSpeed);
        this.smashBlocks();
        if (this.mob.squaredDistanceTo(this.target) <= 9.0D) {
            if (this.mob.tryAttack(this.target)) {
                this.target.damage(this.mob.getDamageSources().mobAttack(this.mob), this.impactDamage);
                this.target.takeKnockback(1.4D, direction.x, direction.z);
            }
            this.charging = 200;
        }
        if (this.charging > 80) {
            this.charging = 0;
            this.cooldown = 160;
        }
    }

    /** Breaks soft blocks the brute runs through. */
    private void smashBlocks() {
        if (this.mob.age % 4 != 0) {
            return;
        }
        BlockPos pos = this.mob.getBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos target = pos.add(dx, 1, dz);
                BlockState state = this.mob.getWorld().getBlockState(target);
                if (state.isOf(Blocks.OAK_LEAVES) || state.isOf(Blocks.GRASS) || state.isOf(Blocks.FERN)
                        || state.isOf(Blocks.TALL_GRASS) || state.isOf(Blocks.VINE) || state.isOf(Blocks.SNOW)) {
                    this.mob.getWorld().breakBlock(target, true, this.mob);
                }
            }
        }
        if (this.mob.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.spawnParticles(CandyParticles.GUMMY_DROPLET, this.mob.getX(), this.mob.getY() + 0.3D, this.mob.getZ(),
                    3, 0.4D, 0.2D, 0.4D, 0.02D);
        }
    }

    /** Exposed for tests/debug: whether the goal is currently charging. */
    public boolean isCharging() {
        return this.windup <= 0 && this.charging > 0 && this.charging < 100;
    }

    /** Kept for parity with vanilla move control usage. */
    @SuppressWarnings("unused")
    private MoveControl moveControl() {
        return this.mob.getMoveControl();
    }
}
