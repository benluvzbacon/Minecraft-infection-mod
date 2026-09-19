package dev.candyinfection.entity;

import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionRuntime;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Lollipop Stalker: a rare, very tall candy creature that only appears in
 * heavily infected regions. It detects players from a long way off, blinks
 * short distances to close gaps, calls in candy minions and unleashes an
 * infection wave that converts the ground around it.
 */
public class LollipopStalkerEntity extends CandyHostileEntity {
    private int teleportCooldown = 60;
    private int waveCooldown = 400;
    private int summonCooldown = 600;

    public LollipopStalkerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createStalkerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 55.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.44D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (--this.teleportCooldown <= 0) {
            this.teleportCooldown = 70 + this.random.nextInt(50);
            this.blinkTowards(world, target);
        }
        if (--this.waveCooldown <= 0) {
            this.waveCooldown = 360;
            this.infectionWave(world);
        }
        if (--this.summonCooldown <= 0) {
            this.summonCooldown = 520;
            this.summonMinions(world);
        }
    }

    /** Short range teleport that keeps line of sight. */
    private void blinkTowards(ServerWorld world, LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            return;
        }
        double step = Math.min(9.0D, Math.max(3.0D, length - 3.0D));
        BlockPos destination = this.getBlockPos().add((int) Math.round(dx / length * step), 0, (int) Math.round(dz / length * step));
        for (int dy = -2; dy <= 3; dy++) {
            BlockPos candidate = destination.up(dy);
            if (world.isAir(candidate) && world.isAir(candidate.up()) && !world.getBlockState(candidate.down()).isAir()) {
                this.requestTeleport(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0F, 0.9F);
                world.spawnParticles(CandyParticles.INFECTION_SPARK, this.getX(), this.getY() + 1.5D, this.getZ(),
                        40, 0.6D, 1.4D, 0.6D, 0.04D);
                return;
            }
        }
    }

    /** Converts the ground in a wide ring around the stalker. */
    private void infectionWave(ServerWorld world) {
        int stage = InfectionWorldState.get(world).getStage();
        int converted = InfectionConversions.infectArea(world, this.getBlockPos(), 8.0D, 0.75F, stage, this.random);
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.HOSTILE, 2.0F, 0.7F);
        world.spawnParticles(CandyParticles.INFECTION_SPARK, this.getX(), this.getY() + 0.5D, this.getZ(),
                120, 6.0D, 1.0D, 6.0D, 0.05D);
        dev.candyinfection.util.CandyLog.debug("Lollipop stalker infection wave converted " + converted + " blocks");
    }

    private void summonMinions(ServerWorld world) {
        for (int i = 0; i < 3; i++) {
            InfectionRuntime.spawnMonster(world, this.getBlockPos().add(this.random.nextInt(9) - 4, 0, this.random.nextInt(9) - 4), false);
        }
        this.playSound(SoundEvents.ENTITY_ENDERMAN_STARE, 1.4F, 0.7F);
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target) {
        target.takeKnockback(0.8D, target.getX() - this.getX(), target.getZ() - this.getZ());
    }

    @Override
    public int candyColor() {
        return 0xFF2D78;
    }

    @Override
    public float infectionPerHit() {
        return super.infectionPerHit() * 1.8F;
    }

    /** Referenced so the entity type is loaded together with its spawn egg. */
    @SuppressWarnings("unused")
    private static EntityType<?> selfType() {
        return CandyEntities.LOLLIPOP_STALKER;
    }
}
