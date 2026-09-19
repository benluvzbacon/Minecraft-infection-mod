package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.effect.StickyEffect;
import dev.candyinfection.effect.SugarCravingEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Status effects used by the infection. They are the language the infection
 * speaks to the player: hunger, being stuck in syrup, being coated in caramel
 * and the (expensive) relief of purification.
 *
 * <p>Effects are exposed as registry entries because 1.21 status effect APIs
 * ({@code StatusEffectInstance}, {@code hasStatusEffect}) take entries.
 */
public final class CandyEffects {
    public static final RegistryEntry.Reference<StatusEffect> SUGAR_CRAVING = register("sugar_craving", new SugarCravingEffect());
    public static final RegistryEntry.Reference<StatusEffect> STICKY = register("sticky", new StickyEffect());
    public static final RegistryEntry.Reference<StatusEffect> CARAMEL_COATED = register("caramel_coated", new CaramelCoatedEffect());
    public static final RegistryEntry.Reference<StatusEffect> PURIFIED = register("purified", new PurifiedEffect());
    public static final RegistryEntry.Reference<StatusEffect> SUGAR_RUSH = register("sugar_rush", new SugarRushEffect());

    private CandyEffects() {
    }

    public static void register() {
        // Static initialisation above performs the registration; this method is the
        // explicit hook called from the mod initialiser so ordering is obvious.
    }

    private static RegistryEntry.Reference<StatusEffect> register(String name, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, CandyInfection.id(name), effect);
    }

    /** Attribute modifier helper shared by the infection system. */
    public static EntityAttributeModifier speedModifier(String name, double amount) {
        return new EntityAttributeModifier(CandyInfection.id(name), amount, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Caramel coated: slow, but the hardened shell protects against fire. */
    private static final class CaramelCoatedEffect extends StatusEffect {
        private CaramelCoatedEffect() {
            super(StatusEffectCategory.HARMFUL, 0xB5651D);
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            return false;
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return duration % 20 == 0;
        }
    }

    /** Purified: regeneration plus immunity to gaining more infection. */
    private static final class PurifiedEffect extends StatusEffect {
        private PurifiedEffect() {
            super(StatusEffectCategory.BENEFICIAL, 0x7CFFD4);
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            if (entity.getHealth() < entity.getMaxHealth() && entity.age % 30 == 0) {
                entity.heal(1.0F + amplifier);
            }
            return true;
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return true;
        }
    }

    /** Sugar rush: the short, tempting burst of speed you get from eating candy. */
    private static final class SugarRushEffect extends StatusEffect {
        private SugarRushEffect() {
            super(StatusEffectCategory.BENEFICIAL, 0xFF5FA2);
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            return false;
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return duration % 40 == 0;
        }
    }
}
