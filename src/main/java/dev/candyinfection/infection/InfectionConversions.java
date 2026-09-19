package dev.candyinfection.infection;

import dev.candyinfection.init.CandyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The infection rule book: which vanilla block turns into which candy block,
 * how likely that is, and how far the infection has to have progressed.
 *
 * <p>The same table also drives purification (the reverse mapping), so a
 * Purifier always restores the block the infection actually replaced.
 */
public final class InfectionConversions {
    /** vanilla block -> conversion rule */
    private static final Map<Block, Conversion> FORWARD = new LinkedHashMap<>();
    /** infected block -> vanilla block it purifies back into */
    private static final Map<Block, Block> REVERSE = new HashMap<>();
    /** every block that counts as "infected" */
    private static final Set<Block> INFECTED = new HashSet<>();
    private static boolean built;

    private InfectionConversions() {
    }

    /** Describes how a vanilla block becomes infected. */
    public record Conversion(Function<BlockState, BlockState> toCandy, float chance, int minStage) {
    }

    /** Builds the table. Called once, after all blocks are registered. */
    public static void build() {
        if (built) {
            return;
        }
        built = true;
        FORWARD.clear();
        REVERSE.clear();
        INFECTED.clear();

        // soils ---------------------------------------------------------------
        add(Blocks.GRASS_BLOCK, CandyBlocks.INFECTED_GRASS_BLOCK, 1.0F, 1);
        add(Blocks.MYCELIUM, CandyBlocks.INFECTED_GRASS_BLOCK, 1.0F, 1);
        add(Blocks.DIRT, CandyBlocks.INFECTED_DIRT, 1.0F, 1);
        add(Blocks.COARSE_DIRT, CandyBlocks.INFECTED_DIRT, 0.9F, 1);
        add(Blocks.PODZOL, CandyBlocks.INFECTED_DIRT, 0.9F, 1);
        add(Blocks.ROOTED_DIRT, CandyBlocks.INFECTED_DIRT, 0.9F, 1);
        add(Blocks.MOSS_BLOCK, CandyBlocks.INFECTED_DIRT, 0.95F, 1);
        add(Blocks.FARMLAND, CandyBlocks.INFECTED_DIRT, 0.7F, 2);
        add(Blocks.MUD, CandyBlocks.STICKY_SYRUP, 0.8F, 2);
        add(Blocks.CLAY, CandyBlocks.CARAMEL_GROWTH, 0.85F, 2);

        // stone ---------------------------------------------------------------
        add(Blocks.STONE, CandyBlocks.CANDY_STONE, 0.85F, 1);
        add(Blocks.COBBLESTONE, CandyBlocks.CANDY_STONE, 0.8F, 1);
        add(Blocks.DEEPSLATE, CandyBlocks.CANDY_STONE, 0.7F, 2);
        add(Blocks.ANDESITE, CandyBlocks.CANDY_STONE, 0.8F, 1);
        add(Blocks.DIORITE, CandyBlocks.CANDY_STONE, 0.8F, 1);
        add(Blocks.GRANITE, CandyBlocks.CANDY_STONE, 0.8F, 1);
        add(Blocks.TUFF, CandyBlocks.CANDY_STONE, 0.7F, 2);
        add(Blocks.STONE_BRICKS, CandyBlocks.CANDY_BRICKS, 0.8F, 3);
        add(Blocks.MOSSY_COBBLESTONE, CandyBlocks.CANDY_BRICKS, 0.7F, 3);
        add(Blocks.AMETHYST_BLOCK, CandyBlocks.SUGAR_CRYSTAL_BLOCK, 0.9F, 3);

        // loose ground --------------------------------------------------------
        add(Blocks.SAND, CandyBlocks.CANDY_SAND, 0.95F, 1);
        add(Blocks.RED_SAND, CandyBlocks.CANDY_SAND, 0.95F, 1);
        add(Blocks.SANDSTONE, CandyBlocks.CANDY_SAND, 0.9F, 2);
        add(Blocks.GRAVEL, CandyBlocks.CANDY_GRAVEL, 0.9F, 1);
        add(Blocks.SNOW_BLOCK, CandyBlocks.STICKY_SYRUP, 0.75F, 3);
        add(Blocks.PACKED_ICE, CandyBlocks.STICKY_SYRUP, 0.6F, 3);

        // wood ----------------------------------------------------------------
        addPillar(Blocks.OAK_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.SPRUCE_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.BIRCH_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.JUNGLE_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.ACACIA_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.DARK_OAK_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.CHERRY_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.MANGROVE_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.STRIPPED_OAK_LOG, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.OAK_WOOD, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.SPRUCE_WOOD, CandyBlocks.GUMMY_LOG);
        addPillar(Blocks.BIRCH_WOOD, CandyBlocks.GUMMY_LOG);
        add(Blocks.OAK_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);
        add(Blocks.SPRUCE_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);
        add(Blocks.BIRCH_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);
        add(Blocks.JUNGLE_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);
        add(Blocks.ACACIA_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);
        add(Blocks.DARK_OAK_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);
        add(Blocks.CHERRY_PLANKS, CandyBlocks.GUMMY_PLANKS, 0.85F, 2);

        // leaves --------------------------------------------------------------
        add(Blocks.OAK_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.SPRUCE_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.BIRCH_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.JUNGLE_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.ACACIA_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.DARK_OAK_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.CHERRY_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.9F, 1);
        add(Blocks.AZALEA_LEAVES, CandyBlocks.GUMMY_LEAVES, 0.85F, 2);

        // plants and crops ----------------------------------------------------
        add(Blocks.SHORT_GRASS, CandyBlocks.GUMMY_GROWTH, 0.9F, 1);
        add(Blocks.FERN, CandyBlocks.GUMMY_GROWTH, 0.9F, 1);
        add(Blocks.TALL_GRASS, CandyBlocks.GUMMY_GROWTH, 0.85F, 1);
        add(Blocks.LARGE_FERN, CandyBlocks.GUMMY_GROWTH, 0.85F, 1);
        add(Blocks.DEAD_BUSH, CandyBlocks.CHOCOLATE_GROWTH, 0.9F, 1);
        add(Blocks.MOSS_CARPET, CandyBlocks.CHOCOLATE_GROWTH, 0.85F, 2);
        add(Blocks.WHEAT, CandyBlocks.GUMMY_GROWTH, 0.6F, 2);
        add(Blocks.CARROTS, CandyBlocks.GUMMY_GROWTH, 0.6F, 2);
        add(Blocks.POTATOES, CandyBlocks.GUMMY_GROWTH, 0.6F, 2);
        add(Blocks.BEETROOTS, CandyBlocks.GUMMY_GROWTH, 0.6F, 2);
        add(Blocks.SUGAR_CANE, CandyBlocks.GUMMY_GROWTH, 0.7F, 2);
        add(Blocks.PUMPKIN, CandyBlocks.CHOCOLATE_BLOB, 0.8F, 2);
        add(Blocks.MELON, CandyBlocks.CHOCOLATE_BLOB, 0.8F, 2);
        add(Blocks.HAY_BLOCK, CandyBlocks.CARAMEL_GROWTH, 0.85F, 2);
        add(Blocks.CACTUS, CandyBlocks.CARAMEL_GROWTH, 0.7F, 3);
        add(Blocks.BROWN_MUSHROOM_BLOCK, CandyBlocks.CHOCOLATE_BLOB, 0.8F, 2);
        add(Blocks.RED_MUSHROOM_BLOCK, CandyBlocks.CHOCOLATE_BLOB, 0.8F, 2);
        add(Blocks.VINE, CandyBlocks.CANDY_VINES, 0.9F, 1);

        // Everything the infection produces is itself infected.
        for (Block block : CandyBlocks.allInfectedBlocks()) {
            INFECTED.add(block);
        }
        // Purification targets for infection-only blocks that have no vanilla origin.
        reverse(CandyBlocks.STICKY_SYRUP, Blocks.AIR);
        reverse(CandyBlocks.SUGAR_CRYSTAL_CLUSTER, Blocks.AIR);
        reverse(CandyBlocks.GUMMY_GROWTH, Blocks.SHORT_GRASS);
        reverse(CandyBlocks.CHOCOLATE_GROWTH, Blocks.DEAD_BUSH);
        reverse(CandyBlocks.LOLLIPOP_PINK, Blocks.AIR);
        reverse(CandyBlocks.LOLLIPOP_BLUE, Blocks.AIR);
        reverse(CandyBlocks.LOLLIPOP_RED, Blocks.AIR);
        reverse(CandyBlocks.LOLLIPOP_YELLOW, Blocks.AIR);
        reverse(CandyBlocks.HARD_CANDY_PINK, Blocks.AIR);
        reverse(CandyBlocks.HARD_CANDY_RED, Blocks.AIR);
        reverse(CandyBlocks.HARD_CANDY_PURPLE, Blocks.AIR);
        reverse(CandyBlocks.HARD_CANDY_CYAN, Blocks.AIR);
        reverse(CandyBlocks.SUGAR_CRYSTAL_ORE, Blocks.STONE);
    }

    private static void add(Block from, Block to, float chance, int minStage) {
        FORWARD.put(from, new Conversion(state -> to.getDefaultState(), chance, minStage));
        reverse(to, from);
        INFECTED.add(to);
    }

    private static void addPillar(Block from, Block to) {
        FORWARD.put(from, new Conversion(state -> {
            BlockState candy = to.getDefaultState();
            if (state.contains(PillarBlock.AXIS)) {
                candy = candy.with(PillarBlock.AXIS, state.get(PillarBlock.AXIS));
            }
            return candy;
        }, 0.85F, 1));
        reverse(to, from);
        INFECTED.add(to);
    }

    private static void reverse(Block candy, Block vanilla) {
        REVERSE.put(candy, vanilla);
    }

    /** @return the rule for infecting {@code block}, or {@code null}. */
    public static Conversion get(Block block) {
        return FORWARD.get(block);
    }

    public static boolean isInfected(Block block) {
        return INFECTED.contains(block);
    }

    public static boolean isInfected(BlockState state) {
        return INFECTED.contains(state.getBlock());
    }

    public static Set<Block> infectedBlocks() {
        return INFECTED;
    }

    /** @return the vanilla block a purifier should restore, or {@code null}. */
    public static Block vanillaForm(Block candyBlock) {
        return REVERSE.get(candyBlock);
    }

    /**
     * Chooses the candy vegetation that should sprout on top of infected soil.
     * Returns {@code null} when nothing should be placed.
     */
    public static BlockState randomVegetation(Random random, int stage) {
        int roll = random.nextInt(100);
        if (roll < 34) {
            return CandyBlocks.GUMMY_GROWTH.getDefaultState();
        }
        if (roll < 58) {
            return CandyBlocks.CHOCOLATE_GROWTH.getDefaultState();
        }
        if (roll < 74) {
            return switch (random.nextInt(4)) {
                case 0 -> CandyBlocks.LOLLIPOP_PINK.getDefaultState();
                case 1 -> CandyBlocks.LOLLIPOP_BLUE.getDefaultState();
                case 2 -> CandyBlocks.LOLLIPOP_RED.getDefaultState();
                default -> CandyBlocks.LOLLIPOP_YELLOW.getDefaultState();
            };
        }
        if (roll < 90) {
            return CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState();
        }
        if (stage >= 3) {
            return CandyBlocks.CHOCOLATE_BLOB.getDefaultState();
        }
        return CandyBlocks.GUMMY_GROWTH.getDefaultState();
    }

    /** Picks one of the four lollipop colours. */
    public static Block randomLollipop(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> CandyBlocks.LOLLIPOP_PINK;
            case 1 -> CandyBlocks.LOLLIPOP_BLUE;
            case 2 -> CandyBlocks.LOLLIPOP_RED;
            default -> CandyBlocks.LOLLIPOP_YELLOW;
        };
    }

    /** Picks one of the four hard candy colours. */
    public static Block randomHardCandy(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> CandyBlocks.HARD_CANDY_PINK;
            case 1 -> CandyBlocks.HARD_CANDY_RED;
            case 2 -> CandyBlocks.HARD_CANDY_PURPLE;
            default -> CandyBlocks.HARD_CANDY_CYAN;
        };
    }

    /**
     * Spreads infection over a spherical volume, used by chocolate creeper
     * explosions, boss attacks and the {@code /candyinfection test} command.
     *
     * @return how many blocks were converted
     */
    public static int infectArea(WorldAccess world, BlockPos center, double radius, float chance, int stage, Random random) {
        int converted = 0;
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            if (center.getSquaredDistance(pos) > radius * radius) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            Conversion conversion = FORWARD.get(state.getBlock());
            if (conversion == null || conversion.minStage() > stage) {
                continue;
            }
            if (random.nextFloat() > chance) {
                continue;
            }
            world.setBlockState(pos, conversion.toCandy().apply(state), Block.NOTIFY_LISTENERS);
            converted++;
        }
        return converted;
    }
}
