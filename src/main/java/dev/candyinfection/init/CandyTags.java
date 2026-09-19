package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/** Tag keys used by the mod and exposed to datapacks. */
public final class CandyTags {
    /** Blocks the infection is allowed to overwrite. Extensible by datapacks. */
    public static final TagKey<Block> INFECTABLE = TagKey.of(RegistryKeys.BLOCK, CandyInfection.id("infectable"));

    /** Blocks that are themselves infection, so tools and purifiers can find them. */
    public static final TagKey<Block> INFECTED = TagKey.of(RegistryKeys.BLOCK, CandyInfection.id("infected"));

    /** Blocks the infection must never overwrite, whatever a datapack says. */
    public static final TagKey<Block> INFECTION_IMMUNE = TagKey.of(RegistryKeys.BLOCK, CandyInfection.id("infection_immune"));

    /** Blocks that a charging Gummy Brute or Jawbreaker smashes through. */
    public static final TagKey<Block> CHARGE_BREAKABLE = TagKey.of(RegistryKeys.BLOCK, CandyInfection.id("charge_breakable"));

    /** Candy fuel the purifier accepts. */
    public static final TagKey<net.minecraft.item.Item> PURIFIER_FUEL =
            TagKey.of(RegistryKeys.ITEM, CandyInfection.id("purifier_fuel"));

    /** Entities immune to infection. */
    public static final TagKey<EntityType<?>> INFECTION_IMMUNE_ENTITY =
            TagKey.of(RegistryKeys.ENTITY_TYPE, CandyInfection.id("infection_immune"));

    private CandyTags() {
    }

    public static void register() {
        dev.candyinfection.util.CandyLog.debug("Tags initialised");
    }
}
