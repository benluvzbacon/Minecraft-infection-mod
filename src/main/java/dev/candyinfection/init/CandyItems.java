package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.item.CandyArmorItem;
import dev.candyinfection.item.CandyArmorMaterials;
import dev.candyinfection.item.CandyBowItem;
import dev.candyinfection.item.CandyFoodItem;
import dev.candyinfection.item.CandyHammerItem;
import dev.candyinfection.item.CandyToolMaterials;
import dev.candyinfection.item.CaramelBladeItem;
import dev.candyinfection.item.CureItem;
import dev.candyinfection.item.GummySpearItem;
import net.minecraft.block.Block;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import dev.candyinfection.item.CandyAxeItem;
import dev.candyinfection.item.CandyHoeItem;
import dev.candyinfection.item.CandyPickaxeItem;
import dev.candyinfection.item.CandyShovelItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rarity;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Item registration: block items for every candy block, crafting materials,
 * food, tools, weapons, armour, cures and spawn eggs.
 */
public final class CandyItems {
    private static final List<Item> ALL = new ArrayList<>();
    private static int count;

    // ---------------------------------------------------------- block items
    public static final Item INFECTED_GRASS_BLOCK = blockItem(CandyBlocks.INFECTED_GRASS_BLOCK);
    public static final Item INFECTED_DIRT = blockItem(CandyBlocks.INFECTED_DIRT);
    public static final Item CANDY_STONE = blockItem(CandyBlocks.CANDY_STONE);
    public static final Item CANDY_BRICKS = blockItem(CandyBlocks.CANDY_BRICKS);
    public static final Item SUGAR_CRYSTAL_ORE = blockItem(CandyBlocks.SUGAR_CRYSTAL_ORE);
    public static final Item SUGAR_CRYSTAL_BLOCK = blockItem(CandyBlocks.SUGAR_CRYSTAL_BLOCK);
    public static final Item SUGAR_CRYSTAL_CLUSTER = blockItem(CandyBlocks.SUGAR_CRYSTAL_CLUSTER);
    public static final Item CANDY_SAND = blockItem(CandyBlocks.CANDY_SAND);
    public static final Item CANDY_GRAVEL = blockItem(CandyBlocks.CANDY_GRAVEL);
    public static final Item STICKY_SYRUP = blockItem(CandyBlocks.STICKY_SYRUP);
    public static final Item CARAMEL_GROWTH = blockItem(CandyBlocks.CARAMEL_GROWTH);
    public static final Item GUMMY_LOG = blockItem(CandyBlocks.GUMMY_LOG);
    public static final Item GUMMY_PLANKS = blockItem(CandyBlocks.GUMMY_PLANKS);
    public static final Item GUMMY_LEAVES = blockItem(CandyBlocks.GUMMY_LEAVES);
    public static final Item GUMMY_GROWTH = blockItem(CandyBlocks.GUMMY_GROWTH);
    public static final Item CHOCOLATE_GROWTH = blockItem(CandyBlocks.CHOCOLATE_GROWTH);
    public static final Item CHOCOLATE_BLOB = blockItem(CandyBlocks.CHOCOLATE_BLOB);
    public static final Item CANDY_VINES = blockItem(CandyBlocks.CANDY_VINES);
    public static final Item LOLLIPOP_PINK = blockItem(CandyBlocks.LOLLIPOP_PINK);
    public static final Item LOLLIPOP_BLUE = blockItem(CandyBlocks.LOLLIPOP_BLUE);
    public static final Item LOLLIPOP_RED = blockItem(CandyBlocks.LOLLIPOP_RED);
    public static final Item LOLLIPOP_YELLOW = blockItem(CandyBlocks.LOLLIPOP_YELLOW);
    public static final Item HARD_CANDY_PINK = blockItem(CandyBlocks.HARD_CANDY_PINK);
    public static final Item HARD_CANDY_RED = blockItem(CandyBlocks.HARD_CANDY_RED);
    public static final Item HARD_CANDY_PURPLE = blockItem(CandyBlocks.HARD_CANDY_PURPLE);
    public static final Item HARD_CANDY_CYAN = blockItem(CandyBlocks.HARD_CANDY_CYAN);
    public static final Item INFECTION_CORE = blockItem(CandyBlocks.INFECTION_CORE);
    public static final Item PURIFIER = blockItem(CandyBlocks.PURIFIER);

    // ----------------------------------------------------------- materials
    public static final Item SUGAR_SHARD = simple("sugar_shard");
    public static final Item HARDENED_SUGAR = simple("hardened_sugar");
    public static final Item GUMMY_RESIN = simple("gummy_resin");
    public static final Item CARAMEL_CHUNK = simple("caramel_chunk");
    public static final Item CHOCOLATE_CORE = rare("chocolate_core");
    public static final Item JAWBREAKER_FRAGMENT = simple("jawbreaker_fragment");
    public static final Item INFECTION_CRYSTAL = rare("infection_crystal");
    public static final Item PURIFICATION_CRYSTAL = rare("purification_crystal");
    public static final Item CANDY_ESSENCE = rare("candy_essence");
    public static final Item HOLY_SUGAR = legendary("holy_sugar");

    // ---------------------------------------------------------------- food
    public static final Item GUMMY_WORM = food("gummy_worm", 4, 0.4F, 2.0F);
    public static final Item CANDY_APPLE = food("candy_apple", 6, 0.6F, 3.0F);
    public static final Item CHOCOLATE_BAR = food("chocolate_bar", 5, 0.5F, 4.0F);
    public static final Item GIANT_LOLLIPOP = food("giant_lollipop", 8, 0.8F, 8.0F);
    public static final Item CARAMEL_APPLE = food("caramel_apple", 7, 0.7F, 5.0F);
    public static final Item SUGAR_COOKIE = food("sugar_cookie", 2, 0.2F, 1.0F);
    public static final Item JAWBREAKER_CANDY = food("jawbreaker_candy", 3, 0.3F, 2.5F);

    // --------------------------------------------------------------- cures
    public static final Item PURIFICATION_POTION = register("purification_potion",
            new CureItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON), 25.0F, false));
    public static final Item ANTI_CANDY_SYRINGE = register("anti_candy_syringe",
            new CureItem(new Item.Settings().maxCount(8).rarity(Rarity.RARE), 60.0F, true));

    // --------------------------------------------------------------- tools
    public static final Item CANDY_PICKAXE = register("candy_pickaxe",
            new CandyPickaxeItem(CandyToolMaterials.SUGAR, new Item.Settings()));
    public static final Item CANDY_AXE = register("candy_axe",
            new CandyAxeItem(CandyToolMaterials.SUGAR, new Item.Settings()));
    public static final Item CANDY_SHOVEL = register("candy_shovel",
            new CandyShovelItem(CandyToolMaterials.SUGAR, new Item.Settings()));
    public static final Item CANDY_HOE = register("candy_hoe",
            new CandyHoeItem(CandyToolMaterials.SUGAR, new Item.Settings()));
    public static final Item CANDY_HAMMER = register("candy_hammer",
            new CandyHammerItem(CandyToolMaterials.HARDENED_SUGAR, new Item.Settings()));

    // ------------------------------------------------------------- weapons
    public static final Item SUGAR_SWORD = register("sugar_sword", new SwordItem(CandyToolMaterials.SUGAR,
            new Item.Settings().maxDamage(CandyToolMaterials.SUGAR.getDurability())
                    .attributeModifiers(SwordItem.createAttributeModifiers(CandyToolMaterials.SUGAR, 3, -2.4F))));
    public static final Item JAWBREAKER_MACE = register("jawbreaker_mace", new SwordItem(CandyToolMaterials.HARDENED_SUGAR,
            new Item.Settings().maxDamage(CandyToolMaterials.HARDENED_SUGAR.getDurability())
                    .attributeModifiers(SwordItem.createAttributeModifiers(CandyToolMaterials.HARDENED_SUGAR, 7, -3.5F))));
    public static final Item CANDY_BOW = register("candy_bow",
            new CandyBowItem(new Item.Settings().maxDamage(384)));
    public static final Item GUMMY_SPEAR = register("gummy_spear",
            new GummySpearItem(CandyToolMaterials.HARDENED_SUGAR, new Item.Settings()));
    public static final Item CHOCOLATE_HAMMER = register("chocolate_hammer",
            new CandyHammerItem(CandyToolMaterials.CONFECTIONER, new Item.Settings()));
    public static final Item CARAMEL_BLADE = register("caramel_blade",
            new CaramelBladeItem(CandyToolMaterials.CONFECTIONER, new Item.Settings()));

    // -------------------------------------------------------------- armour
    public static final Item CANDY_HELMET = armor("candy_helmet", CandyArmorMaterials.CANDY,
            net.minecraft.item.ArmorItem.Type.HELMET, 11, 0.4F);
    public static final Item CANDY_CHESTPLATE = armor("candy_chestplate", CandyArmorMaterials.CANDY,
            net.minecraft.item.ArmorItem.Type.CHESTPLATE, 16, 0.6F);
    public static final Item CANDY_LEGGINGS = armor("candy_leggings", CandyArmorMaterials.CANDY,
            net.minecraft.item.ArmorItem.Type.LEGGINGS, 15, 0.5F);
    public static final Item CANDY_BOOTS = armor("candy_boots", CandyArmorMaterials.CANDY,
            net.minecraft.item.ArmorItem.Type.BOOTS, 13, 0.4F);

    public static final Item HARDENED_SUGAR_HELMET = armor("hardened_sugar_helmet", CandyArmorMaterials.HARDENED_SUGAR,
            net.minecraft.item.ArmorItem.Type.HELMET, 15, 1.2F);
    public static final Item HARDENED_SUGAR_CHESTPLATE = armor("hardened_sugar_chestplate", CandyArmorMaterials.HARDENED_SUGAR,
            net.minecraft.item.ArmorItem.Type.CHESTPLATE, 22, 1.8F);
    public static final Item HARDENED_SUGAR_LEGGINGS = armor("hardened_sugar_leggings", CandyArmorMaterials.HARDENED_SUGAR,
            net.minecraft.item.ArmorItem.Type.LEGGINGS, 20, 1.5F);
    public static final Item HARDENED_SUGAR_BOOTS = armor("hardened_sugar_boots", CandyArmorMaterials.HARDENED_SUGAR,
            net.minecraft.item.ArmorItem.Type.BOOTS, 17, 1.2F);

    public static final Item CONFECTIONER_HELMET = armor("confectioner_helmet", CandyArmorMaterials.CONFECTIONER,
            net.minecraft.item.ArmorItem.Type.HELMET, 18, 3.0F, Rarity.EPIC);
    public static final Item CONFECTIONER_CHESTPLATE = armor("confectioner_chestplate", CandyArmorMaterials.CONFECTIONER,
            net.minecraft.item.ArmorItem.Type.CHESTPLATE, 26, 4.5F, Rarity.EPIC);
    public static final Item CONFECTIONER_LEGGINGS = armor("confectioner_leggings", CandyArmorMaterials.CONFECTIONER,
            net.minecraft.item.ArmorItem.Type.LEGGINGS, 24, 3.8F, Rarity.EPIC);
    public static final Item CONFECTIONER_BOOTS = armor("confectioner_boots", CandyArmorMaterials.CONFECTIONER,
            net.minecraft.item.ArmorItem.Type.BOOTS, 20, 3.0F, Rarity.EPIC);

    // -------------------------------------------------------------- trophy
    public static final Item CONFECTIONER_TROPHY = register("confectioner_trophy",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));

    // --------------------------------------------------------- spawn eggs
    public static final Item CANDY_CRAWLER_SPAWN_EGG = egg("candy_crawler_spawn_egg", CandyEntities.CANDY_CRAWLER, 0xFF3D94, 0x8CFF4F);
    public static final Item GUMMY_SPAWN_SPAWN_EGG = egg("gummy_spawn_spawn_egg", CandyEntities.GUMMY_SPAWN, 0x8CFF4F, 0xFFFFFF);
    public static final Item GUMMY_BRUTE_SPAWN_EGG = egg("gummy_brute_spawn_egg", CandyEntities.GUMMY_BRUTE, 0xFF8C00, 0xFFD700);
    public static final Item SUGAR_LEECH_SPAWN_EGG = egg("sugar_leech_spawn_egg", CandyEntities.SUGAR_LEECH, 0xFFE14F, 0xFF69B4);
    public static final Item CANDY_MIMIC_SPAWN_EGG = egg("candy_mimic_spawn_egg", CandyEntities.CANDY_MIMIC, 0xFF77DD, 0x9B5DE5);
    public static final Item CARAMEL_BEAST_SPAWN_EGG = egg("caramel_beast_spawn_egg", CandyEntities.CARAMEL_BEAST, 0xB5651D, 0xFFA94D);
    public static final Item JAWBREAKER_SPAWN_EGG = egg("jawbreaker_spawn_egg", CandyEntities.JAWBREAKER, 0x9B5DE5, 0x00BBF9);
    public static final Item CHOCOLATE_CREEPER_SPAWN_EGG = egg("chocolate_creeper_spawn_egg", CandyEntities.CHOCOLATE_CREEPER, 0x5C3317, 0xFF69B4);
    public static final Item LOLLIPOP_STALKER_SPAWN_EGG = egg("lollipop_stalker_spawn_egg", CandyEntities.LOLLIPOP_STALKER, 0xFF2D78, 0xFFFFFF);
    public static final Item CONFECTIONER_SPAWN_EGG = egg("confectioner_spawn_egg", CandyEntities.CONFECTIONER, 0xFF1493, 0x5C3317);

    private CandyItems() {
    }

    private static Item simple(String name) {
        return register(name, new Item(new Item.Settings()));
    }

    private static Item rare(String name) {
        return register(name, new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
    }

    private static Item legendary(String name) {
        return register(name, new Item(new Item.Settings().maxCount(16).rarity(Rarity.EPIC).fireproof()));
    }

    private static Item food(String name, int nutrition, float saturation, float infection) {
        FoodComponent component = new FoodComponent.Builder().nutrition(nutrition).saturationModifier(saturation).build();
        return register(name, new CandyFoodItem(new Item.Settings().food(component), infection));
    }

    private static Item blockItem(Block block) {
        String name = Registries.BLOCK.getId(block).getPath();
        return register(name, new BlockItem(block, new Item.Settings()));
    }

    private static Item armor(String name, net.minecraft.registry.entry.RegistryEntry<net.minecraft.item.ArmorMaterial> material,
                              net.minecraft.item.ArmorItem.Type type, int baseDurability, float cleansePerMinute) {
        return armor(name, material, type, baseDurability, cleansePerMinute, Rarity.UNCOMMON);
    }

    private static Item armor(String name, net.minecraft.registry.entry.RegistryEntry<net.minecraft.item.ArmorMaterial> material,
                              net.minecraft.item.ArmorItem.Type type, int baseDurability, float cleansePerMinute, Rarity rarity) {
        return register(name, new CandyArmorItem(material, type,
                new Item.Settings().maxDamage(type.getMaxDamage(baseDurability)).rarity(rarity), cleansePerMinute));
    }

    private static Item egg(String name, net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> type,
                            int primary, int secondary) {
        return register(name, new SpawnEggItem(type, primary, secondary, new Item.Settings()));
    }

    private static <I extends Item> I register(String name, I item) {
        count++;
        ALL.add(item);
        return Registry.register(Registries.ITEM, CandyInfection.id(name), item);
    }

    public static void register() {
        dev.candyinfection.util.CandyLog.debug("Items initialised");
    }

    public static int count() {
        return count;
    }

    public static List<Item> all() {
        return ALL;
    }

    /**
     * Bonus drops when an infected block is broken with candy gear, which is how
     * the player funds their purification effort.
     */
    public static void dropInfectedBonus(ServerWorld world, BlockPos pos) {
        float roll = world.random.nextFloat();
        ItemStack stack;
        if (roll < 0.25F) {
            stack = new ItemStack(SUGAR_SHARD, 1 + world.random.nextInt(2));
        } else if (roll < 0.42F) {
            stack = new ItemStack(GUMMY_RESIN);
        } else if (roll < 0.52F) {
            stack = new ItemStack(CARAMEL_CHUNK);
        } else if (roll < 0.58F) {
            stack = new ItemStack(INFECTION_CRYSTAL);
        } else {
            return;
        }
        world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack));
    }

    /** Referenced by the tool material repair lookup, kept here for discoverability. */
    public static EquipmentSlot mainHand() {
        return EquipmentSlot.MAINHAND;
    }
}
