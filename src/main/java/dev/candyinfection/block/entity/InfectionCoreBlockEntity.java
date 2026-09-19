package dev.candyinfection.block.entity;

import dev.candyinfection.block.InfectionCoreBlock;
import dev.candyinfection.init.CandyBlockEntities;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionCoreLogic;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionRuntime;
import dev.candyinfection.infection.InfectionSpread;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The living part of an infection core.
 *
 * <p>Every core: spreads infection in a wide radius, spawns candy monsters
 * (within the global cap), grows candy structures around itself, emits
 * particles and ambient noise, and absorbs damage in four stages.
 */
public class InfectionCoreBlockEntity extends BlockEntity {
    /** Shield points per stage. */
    public static final int SHIELD_PER_STAGE = 120;

    private int shield = SHIELD_PER_STAGE;
    private int spreadCooldown;
    private int spawnCooldown = 200;
    private int growCooldown = 600;
    private int pulse;

    public InfectionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(CandyBlockEntities.INFECTION_CORE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, InfectionCoreBlockEntity core) {
        core.tick((ServerWorld) world, pos, state);
    }

    private void tick(ServerWorld world, BlockPos pos, BlockState state) {
        this.pulse++;
        InfectionWorldState data = InfectionWorldState.get(world);
        int stage = state.get(InfectionCoreBlock.STAGE);

        // Ambient life: particles + strange noises.
        if (this.pulse % 12 == 0) {
            world.spawnParticles(CandyParticles.INFECTION_SPARK, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    6, 1.4D, 1.2D, 1.4D, 0.02D);
        }
        if (this.pulse % 240 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.AMBIENT, 1.0F,
                    0.6F + world.random.nextFloat() * 0.3F);
        }

        // Spreading: the core is a much stronger source than a normal block.
        // ULTRA FAST per user request - infection should be scary and not underestimated.
        if (--this.spreadCooldown <= 0) {
            this.spreadCooldown = Math.max(1, 10 - stage * 2); // Was 3-18, now 1-10 = much faster
            int attempts = 16 + stage * 8; // Was 8+stage*4, now double
            for (int i = 0; i < attempts; i++) {
                BlockPos target = pos.add(world.random.nextInt(31) - 15, world.random.nextInt(13) - 6, world.random.nextInt(31) - 15);
                BlockState current = world.getBlockState(target);
                if (InfectionConversions.isInfected(current)) {
                    // Already infected: push it into the spread queue so it spreads further.
                    InfectionRuntime.enqueue(world, target);
                    // Also enqueue neighbors for chain reaction
                    if (world.random.nextInt(2) == 0) {
                        BlockPos n = target.add(world.random.nextInt(3) - 1, 0, world.random.nextInt(3) - 1);
                        InfectionRuntime.enqueue(world, n);
                    }
                    continue;
                }
                InfectionConversions.Conversion conversion = InfectionConversions.get(current.getBlock());
                if (conversion != null && conversion.minStage() <= stage) {
                    // Directly convert - 90% chance now (was 75%) for much faster visible spread
                    if (world.random.nextFloat() < 0.90F) {
                        InfectionSpread.convert(world, target, current, conversion.toCandy().apply(current));
                    }
                } else if (world.isAir(target)) {
                    // Grow candy vegetation + lollipops in air pockets - more frequent
                    if (world.random.nextInt(2) == 0) {
                        BlockState growth = InfectionConversions.randomVegetation(world.random, stage);
                        // 40% chance to be lollipop specifically per user request
                        if (world.random.nextInt(100) < 40) {
                            growth = InfectionConversions.randomLollipop(world.random).getDefaultState();
                        }
                        if (growth != null && growth.canPlaceAt(world, target)) {
                            InfectionSpread.convert(world, target, current, growth);
                        }
                    }
                }
            }
            // Keep core's chunk super active
            for (int i = 0; i < 12; i++) {
                BlockPos near = pos.add(world.random.nextInt(13) - 6, world.random.nextInt(5) - 2, world.random.nextInt(13) - 6);
                if (InfectionConversions.isInfected(world.getBlockState(near))) {
                    InfectionRuntime.enqueue(world, near);
                }
            }
        }

        // Monster spawning - more frequent for scarier infection
        if (--this.spawnCooldown <= 0) {
            this.spawnCooldown = Math.max(40, 400 - stage * 100); // Was 80-700, now 40-400 = 2x faster
            InfectionRuntime.spawnMonster(world, pos, false);
            if (stage >= 1) {
                InfectionRuntime.spawnMonster(world, pos, false);
            }
            if (stage >= 3) {
                InfectionRuntime.spawnMonster(world, pos, false);
            }
            if (stage >= 4) {
                InfectionRuntime.spawnMonster(world, pos, true); // Ignore cap at high stage
            }
        }

        // Growing candy structures - now focuses on lollipops and candy, not gummy groves per user
        if (--this.growCooldown <= 0) {
            this.growCooldown = Math.max(100, 1000 - stage * 250); // Was 200-1800, now 100-1000 = faster
            this.growStructure(world, pos, stage);
        }

        if (this.pulse % 100 == 0 && this.shield < SHIELD_PER_STAGE) {
            // The shell slowly repairs itself if left alone.
            this.shield = Math.min(SHIELD_PER_STAGE, this.shield + 2);
            this.markDirty();
        }
    }

    private void growStructure(ServerWorld world, BlockPos pos, int stage) {
        int radius = 12 + stage * 3; // Was 8+stage*2, now larger
        // Convert ground around core to candy - ULTRA aggressive per user request
        for (int i = 0; i < 64 + stage * 24; i++) { // Was 32+stage*16, now double
            BlockPos target = pos.add(world.random.nextInt(radius * 2 + 1) - radius,
                    world.random.nextInt(9) - 4,
                    world.random.nextInt(radius * 2 + 1) - radius);
            BlockState current = world.getBlockState(target);
            InfectionConversions.Conversion conversion = InfectionConversions.get(current.getBlock());
            if (conversion != null && conversion.minStage() <= stage) {
                InfectionSpread.convert(world, target, current, conversion.toCandy().apply(current));
            } else if (world.isAir(target)) {
                // 50% chance to grow something in air - more lollipops per user request
                if (world.random.nextInt(2) == 0) {
                    BlockState growth;
                    int roll = world.random.nextInt(100);
                    if (roll < 45) {
                        // 45% lollipops - user wants lollipops and candy stuff
                        growth = InfectionConversions.randomLollipop(world.random).getDefaultState();
                    } else if (roll < 70) {
                        growth = CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                                .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, world.random.nextInt(3));
                    } else if (roll < 85) {
                        growth = InfectionConversions.randomHardCandy(world.random).getDefaultState();
                    } else {
                        growth = InfectionConversions.randomVegetation(world.random, stage);
                    }
                    if (growth != null && growth.canPlaceAt(world, target)) {
                        InfectionSpread.convert(world, target, current, growth);
                    }
                }
            }
        }
        // Build candy structures - user said "dont add gummy structures" but wants lollipops and candy
        // So we now build ONLY lollipop fields, hard candy arches, sugar spires, chocolate mounds - NO gummy groves
        if (world.random.nextInt(2) == 0) { // Was 1/3 chance, now 1/2 = more frequent
            try {
                String kind = switch (world.random.nextInt(4)) {
                    case 0 -> dev.candyinfection.world.gen.CandyStructures.LOLLIPOP_FIELD; // Most common per user
                    case 1 -> dev.candyinfection.world.gen.CandyStructures.HARD_CANDY_ARCH;
                    case 2 -> dev.candyinfection.world.gen.CandyStructures.SUGAR_SPIRE;
                    default -> dev.candyinfection.world.gen.CandyStructures.CHOCOLATE_MOUND;
                };
                BlockPos structurePos = pos.add(world.random.nextInt(21) - 10, 0, world.random.nextInt(21) - 10);
                dev.candyinfection.world.gen.CandyStructures.build(world, structurePos, kind, InfectionWorldState.get(world));
            } catch (Exception ignored) {
            }
        }
        // Extra lollipop clusters - user specifically wants lollipops
        if (world.random.nextInt(3) == 0) {
            try {
                for (int i = 0; i < 5; i++) {
                    BlockPos lollyPos = pos.add(world.random.nextInt(13) - 6, 0, world.random.nextInt(13) - 6);
                    int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, lollyPos.getX(), lollyPos.getZ());
                    if (y > 1) {
                        BlockPos p = new BlockPos(lollyPos.getX(), y + 1, lollyPos.getZ());
                        if (world.isAir(p)) {
                            world.setBlockState(p, InfectionConversions.randomLollipop(world.random).getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        // CLEANER LOOK: Dedicated grass/dirt cleanup - converts leftover vanilla grass that makes infection look messy
        // Budgeted (30 checks) so it won't crash, but ensures cleaner look per user request
        for (int i = 0; i < 30; i++) {
            BlockPos target = pos.add(world.random.nextInt(17) - 8, world.random.nextInt(5) - 2, world.random.nextInt(17) - 8);
            var cur = world.getBlockState(target);
            var block = cur.getBlock();
            if (block == net.minecraft.block.Blocks.GRASS_BLOCK) {
                world.setBlockState(target, CandyBlocks.INFECTED_GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                InfectionWorldState.get(world).addChunkCount(target, 1);
                InfectionWorldState.get(world).blockInfected();
            } else if (block == net.minecraft.block.Blocks.DIRT || block == net.minecraft.block.Blocks.COARSE_DIRT ||
                       block == net.minecraft.block.Blocks.PODZOL || block == net.minecraft.block.Blocks.ROOTED_DIRT) {
                world.setBlockState(target, CandyBlocks.INFECTED_DIRT.getDefaultState(), Block.NOTIFY_ALL);
                InfectionWorldState.get(world).addChunkCount(target, 1);
                InfectionWorldState.get(world).blockInfected();
            }
        }

        // Nests are important for monster spawning
        if (stage >= 1 && world.random.nextInt(4) == 0) { // Was stage>=2 and 1/6, now stage>=1 and 1/4 = more nests
            try {
                BlockPos nestPos = pos.add(world.random.nextInt(25) - 12, 0, world.random.nextInt(25) - 12);
                dev.candyinfection.world.gen.CandyStructures.build(world, nestPos, dev.candyinfection.world.gen.CandyStructures.INFECTION_NEST, InfectionWorldState.get(world));
            } catch (Exception ignored) {
            }
        }
    }

    /** Registers the core with the world state and immediately starts infection - ULTRA FAST */
    public void onPlaced() {
        if (this.world instanceof ServerWorld serverWorld) {
            InfectionWorldState state = InfectionWorldState.get(serverWorld);
            state.addCore(this.pos);
            // Immediately infect a LARGER radius so players see scary fast infection - user wants it fast
            int initialRadius = 12; // Was 8, now 12
            int converted = 0;
            for (BlockPos target : BlockPos.iterate(this.pos.add(-initialRadius, -4, -initialRadius), this.pos.add(initialRadius, 4, initialRadius))) {
                if (this.pos.getSquaredDistance(target) > initialRadius * initialRadius) continue;
                BlockState current = serverWorld.getBlockState(target);
                if (current.isAir()) continue;
                InfectionConversions.Conversion conv = InfectionConversions.get(current.getBlock());
                if (conv != null) {
                    InfectionSpread.convert(serverWorld, target, current, conv.toCandy().apply(current));
                    converted++;
                    if (converted > 250) break; // Was 120, now 250 = bigger initial burst
                }
            }
            // Place initial candy growth - MORE lollipops per user request
            for (int i = 0; i < 40; i++) { // Was 24, now 40
                BlockPos up = this.pos.add(serverWorld.random.nextInt(13) - 6, 1, serverWorld.random.nextInt(13) - 6);
                if (serverWorld.isAir(up)) {
                    BlockState growth;
                    int roll = serverWorld.random.nextInt(100);
                    if (roll < 50) {
                        growth = InfectionConversions.randomLollipop(serverWorld.random).getDefaultState();
                    } else if (roll < 75) {
                        growth = CandyBlocks.SUGAR_CRYSTAL_CLUSTER.getDefaultState()
                                .with(dev.candyinfection.block.SugarCrystalClusterBlock.SIZE, serverWorld.random.nextInt(3));
                    } else {
                        growth = InfectionConversions.randomVegetation(serverWorld.random, 1);
                    }
                    if (growth != null && growth.canPlaceAt(serverWorld, up)) {
                        serverWorld.setBlockState(up, growth, Block.NOTIFY_ALL);
                    }
                }
            }
            // Enqueue MANY positions so spread continues aggressively
            for (int i = 0; i < 32; i++) { // Was 16, now 32
                BlockPos q = this.pos.add(serverWorld.random.nextInt(15) - 7, serverWorld.random.nextInt(7) - 3, serverWorld.random.nextInt(15) - 7);
                InfectionRuntime.enqueue(serverWorld, q);
            }
            serverWorld.playSound(null, this.pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0F, 0.7F);
            serverWorld.spawnParticles(CandyParticles.INFECTION_SPARK, this.pos.getX() + 0.5D, this.pos.getY() + 1.0D,
                    this.pos.getZ() + 0.5D, 120, 4.0D, 3.0D, 4.0D, 0.08D);
            // Spawn initial defenders - more now for scarier start
            for (int i = 0; i < 3; i++) {
                InfectionRuntime.spawnMonster(serverWorld, this.pos, true);
            }
            // Immediately build a lollipop field around core so it looks infested
            try {
                dev.candyinfection.world.gen.CandyStructures.build(serverWorld, this.pos.add(5, 0, 5),
                        dev.candyinfection.world.gen.CandyStructures.LOLLIPOP_FIELD, state);
                dev.candyinfection.world.gen.CandyStructures.build(serverWorld, this.pos.add(-5, 0, -5),
                        dev.candyinfection.world.gen.CandyStructures.HARD_CANDY_ARCH, state);
            } catch (Exception ignored) {}
        }
        this.markDirty();
    }

    public void onRemoved() {
        if (this.world instanceof ServerWorld serverWorld) {
            InfectionWorldState.get(serverWorld).removeCore(this.pos);
        }
    }

    /**
     * Handles a mining attempt. The core cannot simply be broken: the player has
     to chew through the shell first.
     *
     * @return {@code true} when the break should be allowed
     */
    public boolean onPlayerBreakAttempt(World world, PlayerEntity player, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        int stage = state.get(InfectionCoreBlock.STAGE);
        if (stage >= 4 || this.shield <= 0) {
            return true;
        }
        int damage = damageFrom(player.getMainHandStack());
        this.shield -= damage;
        serverWorld.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.BLOCKS, 1.2F,
                0.8F + serverWorld.random.nextFloat() * 0.4F);
        serverWorld.spawnParticles(CandyParticles.CHOCOLATE_FRAGMENT, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                10, 0.5D, 0.5D, 0.5D, 0.05D);

        if (this.shield <= 0) {
            int nextStage = Math.min(4, stage + 1);
            this.shield = nextStage >= 4 ? 0 : SHIELD_PER_STAGE;
            serverWorld.setBlockState(pos, state.with(InfectionCoreBlock.STAGE, nextStage), Block.NOTIFY_ALL);
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 2.0F, 0.7F);
            serverWorld.spawnParticles(CandyParticles.INFECTION_SPARK, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    80, 2.0D, 2.0D, 2.0D, 0.08D);
            InfectionRuntime.spawnMonster(serverWorld, pos, true);
            player.sendMessage(Text.translatable("message.candyinfection.core_stage", nextStage), true);
        } else {
            player.sendMessage(Text.translatable("message.candyinfection.core_shield",
                    (int) Math.ceil(this.shield * 100.0F / SHIELD_PER_STAGE)), true);
        }
        this.markDirty();
        return false;
    }

    private static int damageFrom(ItemStack stack) {
        if (stack.isEmpty()) {
            return 4;
        }
        if (stack.getItem() instanceof ToolItem tool) {
            return 8 + tool.getMaterial().getEnchantability();
        }
        return 6;
    }

    public int getShield() {
        return this.shield;
    }

    public void setShield(int shield) {
        this.shield = Math.max(0, Math.min(SHIELD_PER_STAGE, shield));
        this.markDirty();
    }

    public Text describe() {
        int stage = this.getCachedState().get(InfectionCoreBlock.STAGE);
        return Text.translatable("message.candyinfection.core_status", stage,
                (int) Math.ceil(this.shield * 100.0F / SHIELD_PER_STAGE));
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Shield", this.shield);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.shield = nbt.contains("Shield") ? nbt.getInt("Shield") : SHIELD_PER_STAGE;
    }

    /** Called by commands for testing. */
    public void weaken() {
        this.setShield(0);
    }

    /** Purification radius granted to neighbouring purifiers (cosmetic stat). */
    public int influenceRadius() {
        return InfectionCoreLogic.PURGE_RADIUS;
    }

    /** Extra candy essence dropped when the core is destroyed by a purifier chain. */
    public ItemStack essenceDrop() {
        return new ItemStack(CandyItems.CANDY_ESSENCE);
    }
}
