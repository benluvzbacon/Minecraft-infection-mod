package dev.candyinfection.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * The Candy Mimic's ambush: it stays perfectly still, disguised, until a player
 * wanders into its trigger radius and then lunges.
 */
public class AmbushGoal extends Goal {
    private final PathAwareEntity mob;
    private final double triggerRadius;
    private boolean triggered;

    public AmbushGoal(PathAwareEntity mob, double triggerRadius) {
        this.mob = mob;
        this.triggerRadius = triggerRadius;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive()
                && this.mob.squaredDistanceTo(target) <= this.triggerRadius * this.triggerRadius;
    }

    @Override
    public void start() {
        this.triggered = true;
        this.mob.setAttacking(true);
    }

    @Override
    public void stop() {
        this.triggered = false;
        this.mob.setAttacking(false);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !this.triggered) {
            return;
        }
        this.mob.getLookControl().lookAt(target, 40.0F, 40.0F);
        if (this.mob.age % 20 == 0 && this.mob.squaredDistanceTo(target) < 64.0D) {
            Vec3d direction = new Vec3d(target.getX() - this.mob.getX(), 0.4D, target.getZ() - this.mob.getZ()).normalize();
            this.mob.addVelocity(direction.x * 0.55D, 0.32D, direction.z * 0.55D);
            this.mob.velocityModified = true;
        }
    }

    public boolean isTriggered() {
        return this.triggered;
    }
}
