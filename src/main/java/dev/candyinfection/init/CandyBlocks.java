package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.block.CandyBlock;
import dev.candyinfection.block.CandyPillarBlock;
import dev.candyinfection.block.CandyPlantBlock;
import dev.candyinfection.block.CandyVineBlock;
import dev.candyinfection.block.CaramelBlock;
import dev.candyinfection.block.GummyLeavesBlock;
import dev.candyinfection.block.HardCandyBlock;
import dev.candyinfection.block.InfectionCoreBlock;
import dev.candyinfection.block.InfectedSoilBlock;
import dev.candyinfection.block.LollipopBlock;
import dev.candyinfection.block.PurifierBlock;
import dev.candyinfection.block.StickySyrupBlock;
import dev.candyinfection.block.SugarCrystalClusterBlock;
import dev.candyinfection.util.CandyLog;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Every block the infection can create, plus the two machines the player uses
 * to fight it (the infection core it destroys and the purifier it builds).
 */
public final class CandyBlocks {
    private static final List<Block> INFECTED_BLOCKS = new ArrayList<>();
    private static int count;

    // soils -----------------------------------------------------------------
    public static final Block INFECTED_GRASS_BLOCK = infected("infected_grass_block",
            new InfectedSoilBlock(settings().strength(0.6F).sounds(BlockSoundGroup.GRASS).mapColor(MapColor.PINK).ticksRandomly()));
    public static final Block INFECTED_DIRT = infected("infected_dirt",
            new InfectedSoilBlock(settings().strength(0.5F).sounds(BlockSoundGroup.GRASS).mapColor(MapColor.MAGENTA).ticksRandomly()));

    // stone -----------------------------------------------------------------
    public static final Block CANDY_STONE = infected("candy_stone",
            new CandyBlock(settings().strength(1.6F, 6.0F).requiresTool().sounds(BlockSoundGroup.STONE).mapColor(MapColor.PINK)));
    public static final Block CANDY_BRICKS = infected("candy_bricks",
            new CandyBlock(settings().strength(1.9F, 7.0F).requiresTool().sounds(BlockSoundGroup.STONE).mapColor(MapColor.DARK_DULL_PINK)));
    public static final Block SUGAR_CRYSTAL_ORE = infected("sugar_crystal_ore",
            new CandyBlock(settings().strength(3.0F, 3.0F).requiresTool().sounds(BlockSoundGroup.STONE).luminance(state -> 4)));
    public static final Block SUGAR_CRYSTAL_BLOCK = infected("sugar_crystal_block",
            new CandyBlock(settings().strength(1.5F).sounds(BlockSoundGroup.AMETHYST_BLOCK).mapColor(MapColor.CYAN)
                    .nonOpaque().luminance(state -> 7)));
    public static final Block SUGAR_CRYSTAL_CLUSTER = infected("sugar_crystal_cluster",
            new SugarCrystalClusterBlock(settings().strength(0.6F).sounds(BlockSoundGroup.AMETHYST_CLUSTER)
                    .nonOpaque().luminance(state -> 4 + state.get(SugarCrystalClusterBlock.SIZE) * 3)));

    // loose ground -----------------------------------------------------------
    public static final Block CANDY_SAND = infected("candy_sand",
            new CandyBlock(settings().strength(0.5F).sounds(BlockSoundGroup.SAND).mapColor(MapColor.PALE_YELLOW)));
    public static final Block CANDY_GRAVEL = infected("candy_gravel",
            new CandyBlock(settings().strength(0.6F).sounds(BlockSoundGroup.GRAVEL).mapColor(MapColor.TERRACOTTA_PINK)));
    public static final Block STICKY_SYRUP = infected("sticky_syrup",
            new StickySyrupBlock(settings().strength(0.4F).sounds(BlockSoundGroup.HONEY).mapColor(MapColor.ORANGE)));
    public static final Block CARAMEL_GROWTH = infected("caramel_growth",
            new CaramelBlock(settings().strength(1.2F).sounds(BlockSoundGroup.HONEY).mapColor(MapColor.TERRACOTTA_ORANGE)));

    // wood -------------------------------------------------------------------
    public static final Block GUMMY_LOG = infected("gummy_log",
            new CandyPillarBlock(settings().strength(2.0F).sounds(BlockSoundGroup.WOOD).mapColor(MapColor.MAGENTA)));
    public static final Block GUMMY_PLANKS = infected("gummy_planks",
            new CandyBlock(settings().strength(2.0F).sounds(BlockSoundGroup.WOOD).mapColor(MapColor.PINK)));
    public static final Block GUMMY_LEAVES = infected("gummy_leaves",
            new GummyLeavesBlock(settings().strength(0.25F).sounds(BlockSoundGroup.AZALEA_LEAVES).mapColor(MapColor.LIME)));

    // vegetation -------------------------------------------------------------
    public static final Block GUMMY_GROWTH = infected("gummy_growth",
            new CandyPlantBlock(settings().strength(0.0F).sounds(BlockSoundGroup.GRASS).mapColor(MapColor.GREEN), 1.2F));
    public static final Block CHOCOLATE_GROWTH = infected("chocolate_growth",
            new CandyPlantBlock(settings().strength(0.0F).sounds(BlockSoundGroup.GRASS).mapColor(MapColor.BROWN), 1.1F));
    public static final Block CHOCOLATE_BLOB = infected("chocolate_blob",
            new CandyBlock(settings().strength(1.0F).sounds(BlockSoundGroup.WOOD).mapColor(MapColor.SPRUCE_BROWN)));
    public static final Block CANDY_VINES = infected("candy_vines",
            new CandyVineBlock(settings().strength(0.0F).sounds(BlockSoundGroup.VINE).mapColor(MapColor.MAGENTA)));
    public static final Block LOLLIPOP_PINK = infected("lollipop_pink", lollipop(MapColor.PINK));
    public static final Block LOLLIPOP_BLUE = infected("lollipop_blue", lollipop(MapColor.LAPIS_BLUE));
    public static final Block LOLLIPOP_RED = infected("lollipop_red", lollipop(MapColor.RED));
    public static final Block LOLLIPOP_YELLOW = infected("lollipop_yellow", lollipop(MapColor.YELLOW));

    // hard candy -------------------------------------------------------------
    public static final Block HARD_CANDY_PINK = infected("hard_candy_pink", hardCandy(MapColor.PINK));
    public static final Block HARD_CANDY_RED = infected("hard_candy_red", hardCandy(MapColor.RED));
    public static final Block HARD_CANDY_PURPLE = infected("hard_candy_purple", hardCandy(MapColor.PURPLE));
    public static final Block HARD_CANDY_CYAN = infected("hard_candy_cyan", hardCandy(MapColor.CYAN));

    // machines ---------------------------------------------------------------
    public static final Block INFECTION_CORE = register("infection_core",
            new InfectionCoreBlock(settings().strength(6.0F, 1200.0F).requiresTool()
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK).mapColor(MapColor.MAGENTA).luminance(state -> 12)));
    public static final Block PURIFIER = register("purifier",
            new PurifierBlock(settings().strength(3.5F).requiresTool().sounds(BlockSoundGroup.METAL)
                    .mapColor(MapColor.TEAL).luminance(state -> 6)));

    private CandyBlocks() {
    }

    private static AbstractBlock.Settings settings() {
        return AbstractBlock.Settings.create();
    }

    private static LollipopBlock lollipop(MapColor color) {
        return new LollipopBlock(settings().strength(0.9F).sounds(BlockSoundGroup.WOOD).mapColor(color).nonOpaque());
    }

    private static HardCandyBlock hardCandy(MapColor color) {
        return new HardCandyBlock(settings().strength(1.8F, 6.0F).requiresTool().sounds(BlockSoundGroup.GLASS)
                .mapColor(color).luminance(state -> 3));
    }

    private static <B extends Block> B infected(String name, B block) {
        INFECTED_BLOCKS.add(block);
        return register(name, block);
    }

    private static <B extends Block> B register(String name, B block) {
        count++;
        return Registry.register(Registries.BLOCK, CandyInfection.id(name), block);
    }

    public static void register() {
        CandyLog.debug("Blocks initialised");
    }

    public static int count() {
        return count;
    }

    /** Every block that counts as infected ground. */
    public static List<Block> allInfectedBlocks() {
        return INFECTED_BLOCKS;
    }

    /** Chooses candy vegetation for a random position. */
    public static net.minecraft.block.BlockState randomVegetation(Random random) {
        return dev.candyinfection.infection.InfectionConversions.randomVegetation(random, 2);
    }
}
