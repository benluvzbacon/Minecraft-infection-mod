package dev.candyinfection.item;

import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Consumable cures: the Purification Potion (cheap, small dose) and the
 * Anti-Candy Syringe (expensive, large dose plus temporary immunity).
 */
public class CureItem extends Item {
    private final float cureAmount;
    private final boolean grantsImmunity;

    public CureItem(Settings settings, float cureAmount, boolean grantsImmunity) {
        super(settings);
        this.cureAmount = cureAmount;
        this.grantsImmunity = grantsImmunity;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            PlayerInfection.cure(player, this.cureAmount);
            if (this.grantsImmunity) {
                player.addStatusEffect(new StatusEffectInstance(CandyEffects.PURIFIED, 1200, 0, false, true));
            } else {
                player.addStatusEffect(new StatusEffectInstance(CandyEffects.PURIFIED, 300, 0, false, true));
            }
        }
        if (user instanceof PlayerEntity player) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 24;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public SoundEvent getDrinkSound() {
        return SoundEvents.ITEM_HONEY_BOTTLE_DRINK;
    }

    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ITEM_HONEY_BOTTLE_DRINK;
    }

    public float getCureAmount() {
        return this.cureAmount;
    }
}
