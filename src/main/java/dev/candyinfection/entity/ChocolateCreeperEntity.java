package dev.candyinfection.entity;

import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionWorldState;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/**
 * Chocolate Creeper: looks like a creeper, detonates like one - but instead of
 * just making a hole it converts everything around the blast into candy. The
 * explosion is the infection, which makes it far more dangerous than the damage
 * it deals.
 */
public class ChocolateCreeperEntity extends CandyHostileEntity {
    private static final TrackedData<Integer> FUSE = DataTracker.registerData(ChocolateCreeperEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int FUSE_LENGTH = 26;

    private int lastFuse;

    public ChocolateCreeperEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createCreeperAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(FUSE, 0);
    }

    @Override
    public void tickMovement() {
        if (this.isAlive()) {
            this.lastFuse = this.dataTracker.get(FUSE);
            LivingEntity target = this.getTarget();
            float distance = target == null ? Float.MAX_VALUE : this.distanceTo(target);
            int fuse = this.dataTracker.get(FUSE);
            if (distance > 5.0F) {
                fuse = 0;
            } else if (fuse <= 0 && target != null) {
                fuse = FUSE_LENGTH;
                this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0F, 0.85F);
            }
            if (fuse > 0) {
                fuse--;
                if (fuse <= 0) {
                    this.detonate();
                }
            }
            this.dataTracker.set(FUSE, fuse);
            if (fuse > 0 && this.getWorld() instanceof ServerWorld world && this.age % 3 == 0) {
                world.spawnParticles(CandyParticles.CHOCOLATE_FRAGMENT, this.getX(), this.getY() + 1.0D, this.getZ(),
                        2, 0.3D, 0.4D, 0.3D, 0.02D);
            }
        }
        super.tickMovement();
    }

    /** The signature move: damage plus mass infection. */
    private void detonate() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 2.6F, Explosion.DestructionType.DESTROY);
        int stage = InfectionWorldState.get(world).getStage();
        int converted = InfectionConversions.infectArea(world, pos, 6.0D, 0.9F, stage, this.random);
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.HOSTILE, 2.0F, 0.8F);
        world.spawnParticles(CandyParticles.INFECTION_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(),
                90, 3.0D, 2.0D, 3.0D, 0.06D);
        // Anyone caught in it gets a dose.
        for (net.minecraft.entity.player.PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(this) <= 64.0D) {
                PlayerInfection.add(player, 6.0F);
            }
        }
        dev.candyinfection.util.CandyLog.debug("Chocolate creeper detonated, infecting " + converted + " blocks");
        this.discard();
    }

    /** @return 0..1 how far through the fuse this creeper is, for rendering. */
    public float getFuseProgress() {
        int fuse = this.dataTracker.get(FUSE);
        return fuse <= 0 ? 0.0F : 1.0F - (float) fuse / FUSE_LENGTH;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("CandyFuse", this.dataTracker.get(FUSE));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(FUSE, nbt.getInt("CandyFuse"));
    }

    @Override
    public float infectionPerHit() {
        return 0.0F;
    }

    @Override
    public int candyColor() {
        return 0x5C3317;
    }
}
