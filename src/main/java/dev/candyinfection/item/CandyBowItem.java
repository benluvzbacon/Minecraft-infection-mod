package dev.candyinfection.item;

import dev.candyinfection.entity.CandyProjectileEntity;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyParticles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;

/**
 * Candy Bow: fires sticky candy projectiles instead of arrows. It needs no
 * ammunition, which makes it the cheap ranged option for holding a colony line.
 */
public class CandyBowItem extends BowItem {
    public CandyBowItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient) {
            return;
        }
        int used = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float progress = BowItem.getPullProgress(used);
        if (progress < 0.1F) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) world;
        CandyProjectileEntity projectile = new CandyProjectileEntity(serverWorld, user);
        projectile.configure(3.0F + progress * 5.0F, 2.0F + progress * 3.0F, 2.0D + progress * 1.5D);
        projectile.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, progress * 2.6F, 1.0F);
        serverWorld.spawnEntity(projectile);
        stack.damage(1, user, net.minecraft.entity.EquipmentSlot.MAINHAND);
        serverWorld.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0F, 0.8F);
        serverWorld.spawnParticles(CandyParticles.SUGAR_SPARKLE, user.getX(), user.getEyeY(), user.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
        if (user instanceof PlayerEntity player) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
    }
}
