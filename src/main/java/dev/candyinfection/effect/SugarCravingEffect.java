package dev.candyinfection.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Sugar Craving: the infection eats your food from the inside. Each tick the
 * player is affected they burn through saturation faster.
 */
public class SugarCravingEffect extends StatusEffect {
    public SugarCravingEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFF6FA8);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof PlayerEntity player) {
            player.addExhaustion(0.08F * (amplifier + 1));
        }
        return true;
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
