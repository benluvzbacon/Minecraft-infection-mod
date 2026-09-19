package dev.candyinfection.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Sticky: syrup-coated. Movement is slowed and jumping is heavily penalised,
 * which makes escaping a caramel beast very hard.
 */
public class StickyEffect extends StatusEffect {
    public StickyEffect() {
        super(StatusEffectCategory.HARMFUL, 0xE0A020);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        return false;
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }
}
