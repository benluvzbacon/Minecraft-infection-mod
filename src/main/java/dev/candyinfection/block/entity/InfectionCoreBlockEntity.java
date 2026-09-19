package dev.candyinfection.block.entity;

import dev.candyinfection.block.InfectionCoreBlock;
import dev.candyinfection.init.CandyBlockEntities;
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
        // FIXED: previously it enqueued random positions that were not infected,
        // so spreadFrom() would immediately reject them. Now we directly convert
        // nearby vanilla blocks and also enqueue already-infected blocks to keep
        // the chain going.
        if (--this.spreadCooldown <= 0) {
            this.spreadCooldown = Math.max(3, 18 - stage * 3);
            int attempts = 8 + stage * 4;
            for (int i = 0; i < attempts; i++) {
                BlockPos target = pos.add(world.random.nextInt(25) - 12, world.random.nextInt(11) - 5, world.random.nextInt(25) - 12);
                BlockState current = world.getBlockState(target);
                if (InfectionConversions.isInfected(current)) {
                    // Already infected: push it into the spread queue so it spreads further.
                    InfectionRuntime.enqueue(world, target);
                    continue;
                }
                InfectionConversions.Conversion conversion = InfectionConversions.get(current.getBlock());
                if (conversion != null && conversion.minStage() <= stage) {
                    // Directly convert - this is what makes the infection actually start.
                    if (world.random.nextFloat() < 0.75F) {
                        InfectionSpread.convert(world, target, current, conversion.toCandy().apply(current));
                    }
                } else if (world.isAir(target)) {
                    // Sometimes grow candy vegetation in air pockets near the core.
                    if (world.random.nextInt(4) == 0) {
                        BlockState growth = InfectionConversions.randomVegetation(world.random, stage);
                        if (growth != null && growth.canPlaceAt(world, target)) {
                            InfectionSpread.convert(world, target, current, growth);
                        }
                    }
                }
            }
            // Also keep the core's own chunk active by enqueuing nearby infected blocks.
            for (int i = 0; i < 6; i++) {
                BlockPos near = pos.add(world.random.nextInt(9) - 4, world.random.nextInt(5) - 2, world.random.nextInt(9) - 4);
                if (InfectionConversions.isInfected(world.getBlockState(near))) {
                    InfectionRuntime.enqueue(world, near);
                }
            }
        }

        // Monster spawning.
        if (--this.spawnCooldown <= 0) {
            this.spawnCooldown = Math.max(80, 700 - stage * 150);
            InfectionRuntime.spawnMonster(world, pos, false);
            if (stage >= 2) {
                InfectionRuntime.spawnMonster(world, pos, false);
            }
            if (stage >= 4) {
                InfectionRuntime.spawnMonster(world, pos, false);
            }
        }

        // Growing candy structures.
        if (--this.growCooldown <= 0) {
            this.growCooldown = Math.max(200, 1800 - stage * 400);
            this.growStructure(world, pos, stage);
        }

        if (this.pulse % 100 == 0 && this.shield < SHIELD_PER_STAGE) {
            // The shell slowly repairs itself if left alone.
            this.shield = Math.min(SHIELD_PER_STAGE, this.shield + 2);
            this.markDirty();
        }
    }

    private void growStructure(ServerWorld world, BlockPos pos, int stage) {
        int radius = 8 + stage * 2;
        // Convert ground around core to candy - more aggressive now.
        for (int i = 0; i < 32 + stage * 16; i++) {
            BlockPos target = pos.add(world.random.nextInt(radius * 2 + 1) - radius,
                    world.random.nextInt(7) - 3,
                    world.random.nextInt(radius * 2 + 1) - radius);
            BlockState current = world.getBlockState(target);
            InfectionConversions.Conversion conversion = InfectionConversions.get(current.getBlock());
            if (conversion != null && conversion.minStage() <= stage) {
                InfectionSpread.convert(world, target, current, conversion.toCandy().apply(current));
            } else if (world.isAir(target) && world.random.nextInt(4) == 0) {
                BlockState growth = InfectionConversions.randomVegetation(world.random, stage);
                if (growth != null && growth.canPlaceAt(world, target)) {
                    InfectionSpread.convert(world, target, current, growth);
                }
            }
        }
        // Occasionally build a real candy structure (gummy grove, arch, mound, spire, lollipop field)
        // and always have a chance to build a nest. This makes the world feel infested.
        if (world.random.nextInt(3) == 0) {
            try {
                String kind = switch (world.random.nextInt(5)) {
                    case 0 -> dev.candyinfection.world.gen.CandyStructures.GUMMY_GROVE;
                    case 1 -> dev.candyinfection.world.gen.CandyStructures.HARD_CANDY_ARCH;
                    case 2 -> dev.candyinfection.world.gen.CandyStructures.CHOCOLATE_MOUND;
                    case 3 -> dev.candyinfection.world.gen.CandyStructures.SUGAR_SPIRE;
                    default -> dev.candyinfection.world.gen.CandyStructures.LOLLIPOP_FIELD;
                };
                BlockPos structurePos = pos.add(world.random.nextInt(17) - 8, 0, world.random.nextInt(17) - 8);
                dev.candyinfection.world.gen.CandyStructures.build(world, structurePos, kind, InfectionWorldState.get(world));
            } catch (Exception ignored) {
                // Structure building is best-effort - never crash the core tick.
            }
        }
        // Nests are rarer but important - they are spawn points for monsters.
        if (stage >= 2 && world.random.nextInt(6) == 0) {
            try {
                BlockPos nestPos = pos.add(world.random.nextInt(21) - 10, 0, world.random.nextInt(21) - 10);
                dev.candyinfection.world.gen.CandyStructures.build(world, nestPos, dev.candyinfection.world.gen.CandyStructures.INFECTION_NEST, InfectionWorldState.get(world));
            } catch (Exception ignored) {
            }
        }
    }

    /** Registers the core with the world state and immediately starts infection. */
    public void onPlaced() {
        if (this.world instanceof ServerWorld serverWorld) {
            InfectionWorldState state = InfectionWorldState.get(serverWorld);
            state.addCore(this.pos);
            // Immediately infect a small radius so players see the infection start
            // without waiting for random ticks. This fixes "broke core and nothing happened".
            int initialRadius = 8;
            int converted = 0;
            for (BlockPos target : BlockPos.iterate(this.pos.add(-initialRadius, -3, -initialRadius), this.pos.add(initialRadius, 3, initialRadius))) {
                if (this.pos.getSquaredDistance(target) > initialRadius * initialRadius) continue;
                BlockState current = serverWorld.getBlockState(target);
                if (current.isAir()) continue;
                InfectionConversions.Conversion conv = InfectionConversions.get(current.getBlock());
                if (conv != null) {
                    InfectionSpread.convert(serverWorld, target, current, conv.toCandy().apply(current));
                    converted++;
                    if (converted > 120) break; // budget initial burst
                }
            }
            // Also place some initial candy growth so it looks infested right away
            for (int i = 0; i < 24; i++) {
                BlockPos up = this.pos.add(serverWorld.random.nextInt(9) - 4, 1, serverWorld.random.nextInt(9) - 4);
                if (serverWorld.isAir(up)) {
                    BlockState growth = InfectionConversions.randomVegetation(serverWorld.random, 1);
                    if (growth != null && growth.canPlaceAt(serverWorld, up)) {
                        serverWorld.setBlockState(up, growth, Block.NOTIFY_ALL);
                    }
                }
            }
            // Enqueue nearby positions so spread continues
            for (int i = 0; i < 16; i++) {
                BlockPos q = this.pos.add(serverWorld.random.nextInt(11) - 5, serverWorld.random.nextInt(5) - 2, serverWorld.random.nextInt(11) - 5);
                InfectionRuntime.enqueue(serverWorld, q);
            }
            serverWorld.playSound(null, this.pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0F, 0.7F);
            serverWorld.spawnParticles(CandyParticles.INFECTION_SPARK, this.pos.getX() + 0.5D, this.pos.getY() + 1.0D,
                    this.pos.getZ() + 0.5D, 80, 3.0D, 2.5D, 3.0D, 0.06D);
            // Spawn an initial defender so breaking core immediately is not trivial
            InfectionRuntime.spawnMonster(serverWorld, this.pos, true);
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
