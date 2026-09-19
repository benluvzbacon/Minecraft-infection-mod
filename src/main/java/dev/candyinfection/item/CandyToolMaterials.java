package dev.candyinfection.item;

import dev.candyinfection.CandyInfection;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

/**
 * Tool materials for the candy progression.
 *
 * <p>Deliberately not stronger than Netherite: candy tools trade raw power for
 * infection-specific bonuses (extra drops from infected blocks, infection
 * resistance) so they are specialised rather than strictly better.
 *
 * <p>Repair ingredients are resolved lazily from the registry, which keeps this
 * enum free of any initialisation cycle with the item registry class.
 */
public enum CandyToolMaterials implements ToolMaterial {
    /** Sugar: iron tier speed, low durability, cheap. */
    SUGAR("sugar_shard", 220, 7.0F, 2.0F, 14, BlockTags.INCORRECT_FOR_IRON_TOOL),
    /** Hardened sugar: diamond tier speed, medium durability. */
    HARDENED_SUGAR("hardened_sugar", 780, 9.0F, 3.2F, 18, BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
    /** Confectioner: netherite tier speed but less attack damage, very durable. */
    CONFECTIONER("chocolate_core", 2100, 10.0F, 3.0F, 22, BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    private final String repairItemId;
    private final int durability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final TagKey<Block> inverseTag;

    CandyToolMaterials(String repairItemId, int durability, float miningSpeed, float attackDamage,
                       int enchantability, TagKey<Block> inverseTag) {
        this.repairItemId = repairItemId;
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.inverseTag = inverseTag;
    }

    @Override
    public int getDurability() {
        return this.durability;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return this.miningSpeed;
    }

    @Override
    public float getAttackDamage() {
        return this.attackDamage;
    }

    @Override
    public TagKey<Block> getInverseTag() {
        return this.inverseTag;
    }

    @Override
    public int getEnchantability() {
        return this.enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Registries.ITEM.get(CandyInfection.id(this.repairItemId)));
    }
}
