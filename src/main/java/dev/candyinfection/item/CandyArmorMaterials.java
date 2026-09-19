package dev.candyinfection.item;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.init.CandyItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Armor materials. Higher tiers give infection resistance and special abilities
 * rather than raw protection numbers above Netherite.
 */
public final class CandyArmorMaterials {
    /** Candy: leather-ish protection, cheap, small infection resistance. */
    public static final RegistryEntry<ArmorMaterial> CANDY = register("candy",
            defense(1, 3, 2, 1), 12, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, () -> Ingredient.ofItems(CandyItems.SUGAR_SHARD),
            0.0F, 0.0F);

    /** Hardened sugar: iron-ish protection with real toughness. */
    public static final RegistryEntry<ArmorMaterial> HARDENED_SUGAR = register("hardened_sugar",
            defense(2, 6, 5, 2), 14, SoundEvents.ITEM_ARMOR_EQUIP_IRON, () -> Ingredient.ofItems(CandyItems.HARDENED_SUGAR),
            1.0F, 0.0F);

    /** Confectioner: boss-tier protection with knockback resistance. */
    public static final RegistryEntry<ArmorMaterial> CONFECTIONER = register("confectioner",
            defense(3, 8, 6, 3), 20, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, () -> Ingredient.ofItems(CandyItems.CHOCOLATE_CORE),
            3.0F, 0.15F);

    private CandyArmorMaterials() {
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        return Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.BOOTS, boots);
            map.put(ArmorItem.Type.LEGGINGS, leggings);
            map.put(ArmorItem.Type.CHESTPLATE, chestplate);
            map.put(ArmorItem.Type.HELMET, helmet);
            map.put(ArmorItem.Type.BODY, 0);
        });
    }

    private static RegistryEntry<ArmorMaterial> register(String name, EnumMap<ArmorItem.Type, Integer> defense,
                                                         int enchantability, RegistryEntry<net.minecraft.sound.SoundEvent> equipSound,
                                                         Supplier<Ingredient> repairIngredient, float toughness,
                                                         float knockbackResistance) {
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(CandyInfection.id(name)));
        ArmorMaterial material = new ArmorMaterial(defense, enchantability, equipSound, repairIngredient, layers,
                toughness, knockbackResistance);
        return Registry.registerReference(Registries.ARMOR_MATERIAL, CandyInfection.id(name), material);
    }
}
