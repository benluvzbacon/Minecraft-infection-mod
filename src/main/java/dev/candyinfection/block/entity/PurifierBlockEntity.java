package dev.candyinfection.block.entity;

import dev.candyinfection.init.CandyBlockEntities;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Slowly scrubs infection out of the blocks around it, at the cost of fuel.
 * This is the player's main tool for reclaiming land.
 */
public class PurifierBlockEntity extends BlockEntity {
    /** Radius in blocks. */
    public static final int RADIUS = 7;
    /** Blocks cleaned per operation. */
    private static final int CLEAN_PER_OP = 3;

    private int fuel;
    private int cooldown;

    public PurifierBlockEntity(BlockPos pos, BlockState state) {
        super(CandyBlockEntities.PURIFIER, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, PurifierBlockEntity purifier) {
        purifier.tick((ServerWorld) world, pos);
    }

    private void tick(ServerWorld world, BlockPos pos) {
        if (this.fuel <= 0) {
            return;
        }
        if (--this.cooldown > 0) {
            return;
        }
        this.cooldown = 10;
        int cleaned = 0;
        for (int i = 0; i < CLEAN_PER_OP * 8 && cleaned < CLEAN_PER_OP; i++) {
            BlockPos target = pos.add(world.random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    world.random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    world.random.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockState targetState = world.getBlockState(target);
            Block vanilla = InfectionConversions.vanillaForm(targetState.getBlock());
            if (vanilla == null) {
                continue;
            }
            world.setBlockState(target, vanilla.getDefaultState(), Block.NOTIFY_ALL);
            InfectionWorldState data = InfectionWorldState.get(world);
            data.addChunkCount(target, -1);
            data.blockPurified();
            world.spawnParticles(CandyParticles.PURIFICATION_SPARK, target.getX() + 0.5D, target.getY() + 0.5D,
                    target.getZ() + 0.5D, 4, 0.3D, 0.3D, 0.3D, 0.01D);
            cleaned++;
        }
        if (cleaned > 0) {
            this.fuel -= cleaned;
            this.markDirty();
            if (world.random.nextInt(20) == 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.5F, 1.4F);
            }
        }
    }

    /** @return how much fuel the given stack is worth, or 0 when it is not fuel. */
    public static int fuelValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.isOf(CandyItems.HOLY_SUGAR)) {
            return 120;
        }
        if (stack.isOf(CandyItems.PURIFICATION_CRYSTAL)) {
            return 60;
        }
        if (stack.isOf(CandyItems.HARDENED_SUGAR)) {
            return 12;
        }
        if (stack.isOf(CandyItems.SUGAR_SHARD)) {
            return 4;
        }
        return 0;
    }

    public void addFuel(int amount) {
        this.fuel = Math.min(4000, this.fuel + amount);
        this.markDirty();
    }

    public int getFuel() {
        return this.fuel;
    }

    public Text describe() {
        return Text.translatable("message.candyinfection.purifier_status", this.fuel, RADIUS);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Fuel", this.fuel);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.fuel = nbt.getInt("Fuel");
    }
}
