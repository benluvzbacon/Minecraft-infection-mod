package dev.candyinfection.entity;

import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.infection.InfectionConversions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Sugar Leech: small, low to the ground and easy to miss in a dense colony. It
 * only commits to an attack once you are very close, drains your food and
 * saturates you with infection.
 */
public class SugarLeechEntity extends CandyHostileEntity {
    public SugarLeechEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createLeechAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0D);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        // Hides until the player is almost on top of it.
        return super.canTarget(target) && this.squaredDistanceTo(target) <= 64.0D;
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target) {
        target.addStatusEffect(new StatusEffectInstance(CandyEffects.SUGAR_CRAVING, 240, 1, false, true));
        if (target instanceof PlayerEntity player) {
            player.addExhaustion(2.5F);
        }
        this.playSound(SoundEvents.ENTITY_SLIME_ATTACK, 0.8F, 1.5F);
        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            dev.candyinfection.infection.InfectionSpread.onInfectedBlockTick(serverWorld, this.getBlockPos().down(),
                    serverWorld.getBlockState(this.getBlockPos().down()), 4.0F);
        }
    }

    @Override
    public float infectionPerHit() {
        return super.infectionPerHit() * 2.0F;
    }

    @Override
    public int candyColor() {
        return 0xFFE14F;
    }

    /** Kept here so the block conversion table is loaded with this entity. */
    @SuppressWarnings("unused")
    private static boolean soilCheck(net.minecraft.block.BlockState state) {
        return InfectionConversions.isInfected(state);
    }
}
