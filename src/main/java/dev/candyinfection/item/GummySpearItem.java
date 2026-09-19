package dev.candyinfection.item;

import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Gummy Spear: right-click to lunge. It throws the player forward and skewers
 * everything in a line in front of them, which is the reliable way to interrupt
 * a Gummy Brute charge.
 */
public class GummySpearItem extends SwordItem {
    private static final float LUNGE_DAMAGE = 7.0F;

    public GummySpearItem(ToolMaterial material, Settings settings) {
        super(material, new Settings()
                .maxDamage(material.getDurability())
                .attributeModifiers(SwordItem.createAttributeModifiers(material, 4, -2.6F)));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        Vec3d look = user.getRotationVec(1.0F);
        user.addVelocity(look.x * 0.85D, 0.14D, look.z * 0.85D);
        user.velocityModified = true;
        Box area = user.getBoundingBox().stretch(look.multiply(4.5D)).expand(1.2D);
        int hits = 0;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity != user && entity.isAlive())) {
            if (target.damage(user.getDamageSources().playerAttack(user), LUNGE_DAMAGE)) {
                target.takeKnockback(0.9D, -look.x, -look.z);
                target.addStatusEffect(new StatusEffectInstance(CandyEffects.STICKY, 100, 0, false, true));
                hits++;
            }
        }
        if (hits > 0) {
            PlayerInfection.add(user, 1.0F);
        }
        user.playSound(SoundEvents.ITEM_TRIDENT_THROW.value(), 1.0F, 1.2F);
        stack.damage(2, user, EquipmentSlot.MAINHAND);
        user.getItemCooldownManager().set(this, 45);
        return TypedActionResult.consume(stack);
    }
}
