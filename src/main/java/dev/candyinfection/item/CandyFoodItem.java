package dev.candyinfection.item;

import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Candy food: the risk/reward mechanic. Every candy item restores hunger and
 * gives a short sugar rush, but it also feeds the infection.
 */
public class CandyFoodItem extends Item {
    private final float infection;

    public CandyFoodItem(Settings settings, float infection) {
        super(settings);
        this.infection = infection;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient && user instanceof PlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(CandyEffects.SUGAR_RUSH, 240, 0, false, true));
            PlayerInfection.add(player, this.infection);
        }
        return result;
    }

    /** How much infection this food adds when eaten. */
    public float getInfection() {
        return this.infection;
    }
}
