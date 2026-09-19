package dev.candyinfection.entity;

import dev.candyinfection.entity.goal.AmbushGoal;
import dev.candyinfection.init.CandyBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Candy Mimic: disguises itself as a candy block and waits. It does not move or
 * target anything until a player comes within a few blocks, then it lunges.
 *
 * <p>The disguise is a synced data tracker entry so clients render the right
 * block and the trick actually works in multiplayer.
 */
public class CandyMimicEntity extends CandyHostileEntity {
    private static final TrackedData<Integer> DISGUISE = DataTracker.registerData(CandyMimicEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Blocks the mimic can pretend to be. */
    public static final Block[] DISGUISES = {
            CandyBlocks.HARD_CANDY_PINK, CandyBlocks.SUGAR_CRYSTAL_BLOCK, CandyBlocks.CHOCOLATE_BLOB,
            CandyBlocks.LOLLIPOP_PINK, CandyBlocks.GUMMY_GROWTH, CandyBlocks.CARAMEL_GROWTH
    };

    public CandyMimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DISGUISE, this.random.nextInt(DISGUISES.length));
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(2, new AmbushGoal(this, 3.5D));
    }

    public Block getDisguiseBlock() {
        int index = this.dataTracker.get(DISGUISE);
        return DISGUISES[Math.floorMod(index, DISGUISES.length)];
    }

    public int getDisguiseIndex() {
        return this.dataTracker.get(DISGUISE);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        // Freeze completely while disguised.
        if (this.getTarget() == null || this.squaredDistanceTo(this.getTarget()) > 16.0D) {
            this.setVelocity(Vec3d.ZERO);
            this.navigation.stop();
        }
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target) {
        this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 1.4F);
        target.takeKnockback(0.6D, this.getX() - target.getX(), this.getZ() - target.getZ());
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Disguise", this.getDisguiseIndex());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DISGUISE, nbt.getInt("Disguise"));
    }

    @Override
    public int candyColor() {
        return 0xFF77DD;
    }

    @Override
    public float infectionPerHit() {
        return super.infectionPerHit() * 1.5F;
    }
}
