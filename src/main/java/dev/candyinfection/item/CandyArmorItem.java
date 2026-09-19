package dev.candyinfection.item;

import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

/**
 * Candy armor. Higher tiers actively scrub infection out of the wearer, which is
 * the "specialised against the infection" payoff for the armour progression.
 */
public class CandyArmorItem extends ArmorItem {
    private final float cleansePerMinute;

    public CandyArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings, float cleansePerMinute) {
        super(material, type, settings);
        this.cleansePerMinute = cleansePerMinute;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || this.cleansePerMinute <= 0.0F || !(entity instanceof PlayerEntity player)) {
            return;
        }
        // Once every three seconds, shave a little infection off.
        if (entity.age % 60 != 0 || !isEquipped(player, stack)) {
            return;
        }
        PlayerInfection.add(player, -this.cleansePerMinute / 20.0F);
    }

    private static boolean isEquipped(LivingEntity entity, ItemStack stack) {
        for (ItemStack equipped : entity.getArmorItems()) {
            if (equipped == stack) {
                return true;
            }
        }
        return false;
    }

    /** Total infection cleanse per minute across everything the player wears. */
    public static float cleanseRate(PlayerEntity player) {
        float total = 0.0F;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem() instanceof CandyArmorItem armor) {
                total += armor.cleansePerMinute;
            }
        }
        return total;
    }
}
