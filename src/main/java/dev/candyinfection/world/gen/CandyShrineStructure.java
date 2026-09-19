package dev.candyinfection.world.gen;

import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * A starting structure that spawns naturally so players can find the infection
 * without commands. It's a candy shrine / altar that contains an infection core
 * surrounded by lollipops, hard candy, sugar crystals and other candy goodies.
 *
 * This is what the user meant by "a structure that spawned so you can start the
 * infection yourself" - not the gummy groves that grow as infection spreads.
 */
public final class CandyShrineStructure {
    private CandyShrineStructure() {}

    /**
     * Builds a candy shrine centered at ground level.
     * @param world server world
     * @param center ground position (shrine base will be at center)
     * @param state infection world state for bookkeeping
     * @return number of blocks placed
     */
    public static int build(ServerWorld world, BlockPos center, InfectionWorldState state) {
        Random random = world.random;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int changed = 0;

        // Find ground
        int surfaceY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        if (surfaceY <= world.getBottomY() + 2) return 0;
        BlockPos ground = new BlockPos(center.getX(), surfaceY, center.getZ());

        // Check if area is suitable - needs solid ground, not water/lava
        BlockState groundState = world.getBlockState(ground.down());
        if (groundState.isAir() || groundState.isOf(Blocks.WATER) || groundState.isOf(Blocks.LAVA)) {
            return 0;
        }

        // Clear area above ground for shrine
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= 6; dy++) {
                    pos.set(ground.getX() + dx, ground.getY() + dy, ground.getZ() + dz);
                    if (!world.getBlockState(pos).isAir() && world.getBlockState(pos).getBlock() != Blocks.WATER) {
                        // Only clear if replaceable (grass, leaves, etc)
                        if (world.getBlockState(pos).isReplaceable() || world.getBlockState(pos).isOf(Blocks.SHORT_GRASS) ||
                            world.getBlockState(pos).isOf(Blocks.TALL_GRASS) || world.getBlockState(pos).isOf(Blocks.OAK_LEAVES) ||
                            world.getBlockState(pos).isOf(Blocks.BIRCH_LEAVES)) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }

        // Base platform - 9x9 candy bricks with sugar crystal corners
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int dist = Math.abs(dx) + Math.abs(dz);
                if (dist > 6) continue;
                pos.set(ground.getX() + dx, ground.getY(), ground.getZ() + dz);
                BlockState baseBlock;
                if (Math.abs(dx) == 4 && Math.abs(dz) == 4) {
                    baseBlock = CandyBlocks.SUGAR_CRYSTAL_BLOCK.getDefaultState();
                } else if (dist <= 2) {
                    baseBlock = CandyBlocks.SUGAR_CRYSTAL_BLOCK.getDefaultState();
                } else {
                    baseBlock = CandyBlocks.CANDY_BRICKS.getDefaultState();
                }
                changed += set(world, pos, baseBlock, state);

                // Second layer for inner ring - candy stone
                if (dist <= 3 && dist > 0) {
                    pos.set(ground.getX() + dx, ground.getY() - 1, ground.getZ() + dz);
                    if (!world.getBlockState(pos).isOf(CandyBlocks.CANDY_BRICKS) &&
                        !world.getBlockState(pos).isOf(CandyBlocks.SUGAR_CRYSTAL_BLOCK)) {
                        changed += set(world, pos, CandyBlocks.CANDY_STONE.getDefaultState(), state);
                    }
                }
            }
        }

        // Hard candy pillars at corners (4 corners)
        int[][] corners = {{-3, -3}, {-3, 3}, {3, -3}, {3, 3}};
        for (int[] corner : corners) {
            int cx = ground.getX() + corner[0];
            int cz = ground.getZ() + corner[1];
            BlockState hardCandy = InfectionConversions.randomHardCandy(random).getDefaultState();
            for (int dy = 1; dy <= 3 + random.nextInt(2); dy++) {
                pos.set(cx, ground.getY() + dy, cz);
                changed += set(world, pos, hardCandy, state);
            }
            // Sugar crystal cluster on top of pillar
            pos.set(cx, ground.getY() + 4 + random.nextInt(2), cz);
            changed += set(world, pos, CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                    .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, random.nextInt(3)), state);
        }

        // Lollipops - 4 cardinal directions, 2 blocks high, different colors
        int[][] lollipopPos = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        Block[] lollipops = {CandyBlocks.LOLLIPOP_PINK, CandyBlocks.LOLLIPOP_BLUE, CandyBlocks.LOLLIPOP_RED, CandyBlocks.LOLLIPOP_YELLOW};
        for (int i = 0; i < 4; i++) {
            int lx = ground.getX() + lollipopPos[i][0];
            int lz = ground.getZ() + lollipopPos[i][1];
            Block lollipop = lollipops[i % lollipops.length];
            for (int dy = 1; dy <= 2 + random.nextInt(2); dy++) {
                pos.set(lx, ground.getY() + dy, lz);
                changed += set(world, pos, lollipop.getDefaultState(), state);
            }
        }

        // More lollipops scattered around outer ring
        for (int i = 0; i < 6; i++) {
            int lx = ground.getX() + random.nextInt(9) - 4;
            int lz = ground.getZ() + random.nextInt(9) - 4;
            if (Math.abs(lx - ground.getX()) + Math.abs(lz - ground.getZ()) < 3) continue;
            if (Math.abs(lx - ground.getX()) + Math.abs(lz - ground.getZ()) > 6) continue;
            Block lollipop = InfectionConversions.randomLollipop(random);
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, lx, lz);
            if (y <= 1) continue;
            pos.set(lx, y + 1, lz);
            changed += set(world, pos, lollipop.getDefaultState(), state);
            if (random.nextBoolean()) {
                pos.set(lx, y + 2, lz);
                changed += set(world, pos, lollipop.getDefaultState(), state);
            }
        }

        // Central altar - 3x3 sugar crystal blocks with infection core on top
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pos.set(ground.getX() + dx, ground.getY() + 1, ground.getZ() + dz);
                if (dx == 0 && dz == 0) {
                    // Core will be placed at y+2
                    changed += set(world, pos, CandyBlocks.SUGAR_CRYSTAL_BLOCK.getDefaultState(), state);
                } else {
                    changed += set(world, pos, CandyBlocks.HARD_CANDY_PURPLE.getDefaultState(), state);
                }
            }
        }

        // Infection core in center - this is what starts the infection
        pos.set(ground.getX(), ground.getY() + 2, ground.getZ());
        changed += set(world, pos, CandyBlocks.INFECTION_CORE.getDefaultState(), state);

        // Surrounding candy goodies - chocolate blobs, caramel, sticky syrup, gummy growth
        for (int i = 0; i < 20; i++) {
            int sx = ground.getX() + random.nextInt(9) - 4;
            int sz = ground.getZ() + random.nextInt(9) - 4;
            int dist = Math.abs(sx - ground.getX()) + Math.abs(sz - ground.getZ());
            if (dist < 2 || dist > 5) continue;
            int sy = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, sx, sz);
            if (sy <= 1) continue;
            pos.set(sx, sy + 1, sz);
            if (!world.getBlockState(pos).isAir()) continue;

            BlockState candy;
            int roll = random.nextInt(100);
            if (roll < 25) {
                candy = CandyBlocks.CHOCOLATE_BLOB.getDefaultState();
            } else if (roll < 45) {
                candy = CandyBlocks.CARAMEL_GROWTH.getDefaultState();
            } else if (roll < 65) {
                candy = CandyBlocks.STICKY_SYRUP.getDefaultState();
            } else if (roll < 80) {
                candy = CandyBlocks.GUMMY_GROWTH.getDefaultState();
            } else {
                candy = CandyBlocks.CHOCOLATE_GROWTH.getDefaultState();
            }
            changed += set(world, pos, candy, state);
        }

        // Sugar crystal clusters scattered
        for (int i = 0; i < 8; i++) {
            int sx = ground.getX() + random.nextInt(7) - 3;
            int sz = ground.getZ() + random.nextInt(7) - 3;
            int sy = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, sx, sz);
            if (sy <= 1) continue;
            pos.set(sx, sy + 1, sz);
            if (!world.getBlockState(pos).isAir()) continue;
            changed += set(world, pos, CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                    .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, random.nextInt(3)), state);
        }

        // Gummy logs as decorative arches
        if (random.nextBoolean()) {
            for (int dx = -2; dx <= 2; dx++) {
                pos.set(ground.getX() + dx, ground.getY() + 4, ground.getZ() - 4);
                changed += set(world, pos, CandyBlocks.GUMMY_LOG.getDefaultState(), state);
                pos.set(ground.getX() + dx, ground.getY() + 4, ground.getZ() + 4);
                changed += set(world, pos, CandyBlocks.GUMMY_LOG.getDefaultState(), state);
            }
        }

        // Chest with candy loot in front of shrine
        try {
            pos.set(ground.getX(), ground.getY() + 1, ground.getZ() + 3);
            if (world.getBlockState(pos).isAir() || world.getBlockState(pos).isReplaceable()) {
                world.setBlockState(pos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_LISTENERS);
                // Fill chest with candy loot if block entity exists
                if (world.getBlockEntity(pos) instanceof net.minecraft.block.entity.ChestBlockEntity chest) {
                    // Add some candy items
                    chest.setStack(0, new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.SUGAR_SHARD, 8 + random.nextInt(8)));
                    chest.setStack(1, new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.CANDY_ESSENCE, 2 + random.nextInt(3)));
                    chest.setStack(2, new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.CHOCOLATE_BAR, 3 + random.nextInt(4)));
                    chest.setStack(3, new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.PURIFICATION_CRYSTAL, 1));
                    chest.setStack(4, new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.GUMMY_WORM, 2 + random.nextInt(3)));
                    if (random.nextBoolean()) {
                        chest.setStack(5, new net.minecraft.item.ItemStack(dev.candyinfection.init.CandyItems.CANDY_APPLE, 1));
                    }
                }
                changed++;
            }
        } catch (Exception ignored) {}

        // Register core and initial infection
        state.addCore(new BlockPos(ground.getX(), ground.getY() + 2, ground.getZ()));
        
        // Immediately infect nearby area so shrine is visibly spreading when found
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (random.nextInt(3) != 0) continue;
                int x = ground.getX() + dx;
                int z = ground.getZ() + dz;
                int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z);
                if (y <= 1) continue;
                BlockPos target = new BlockPos(x, y, z);
                BlockState current = world.getBlockState(target);
                InfectionConversions.Conversion conv = InfectionConversions.get(current.getBlock());
                if (conv != null && random.nextFloat() < 0.6f) {
                    world.setBlockState(target, conv.toCandy().apply(current), Block.NOTIFY_LISTENERS);
                    state.addChunkCount(target, 1);
                    state.blockInfected();
                    changed++;
                }
            }
        }

        return changed;
    }

    private static int set(ServerWorld world, BlockPos pos, BlockState state, InfectionWorldState record) {
        if (state == null) return 0;
        BlockState existing = world.getBlockState(pos);
        if (!existing.isAir() && !existing.isReplaceable() && !existing.isOf(Blocks.SHORT_GRASS) && 
            !existing.isOf(Blocks.TALL_GRASS) && !existing.isOf(Blocks.FERN)) {
            // Don't overwrite solid blocks except for ground layer
            if (pos.getY() > world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ())) {
                return 0;
            }
        }
        if (!world.setBlockState(pos, state, Block.NOTIFY_LISTENERS)) {
            return 0;
        }
        if (InfectionConversions.isInfected(state)) {
            record.addChunkCount(pos, 1);
            record.blockInfected();
        }
        return 1;
    }
}
