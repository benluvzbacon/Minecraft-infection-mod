package dev.candyinfection.infection;

import dev.candyinfection.block.InfectionCoreBlock;
import dev.candyinfection.block.entity.InfectionCoreBlockEntity;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Server side rules for the Candy Infection Core: staged destruction and the
 * purge that follows a core being destroyed.
 */
public final class InfectionCoreLogic {
    /** Radius of the purge zone created when a core dies. */
    public static final int PURGE_RADIUS = 48;
    /** How long the purge keeps scrubbing (in ticks). */
    public static final int PURGE_TICKS = 6000;

    private InfectionCoreLogic() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(state.getBlock() instanceof InfectionCoreBlock)) {
                return true;
            }
            if (blockEntity instanceof InfectionCoreBlockEntity core) {
                return core.onPlayerBreakAttempt(world, player, pos, state);
            }
            return true;
        });
    }

    /** Called when a core actually breaks. */
    public static void onCoreDestroyed(World world, BlockPos pos, net.minecraft.block.BlockState state, PlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        InfectionWorldState data = InfectionWorldState.get(serverWorld);
        data.removeCore(pos);
        data.addPurge(pos, PURGE_RADIUS, PURGE_TICKS);

        serverWorld.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 3.0F, 0.6F);
        serverWorld.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.0F, 1.4F);
        serverWorld.spawnParticles(CandyParticles.PURIFICATION_SPARK, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                180, 6.0D, 4.0D, 6.0D, 0.06D);

        dropLoot(serverWorld, pos, state.get(InfectionCoreBlock.STAGE));
        for (PlayerEntity target : serverWorld.getPlayers()) {
            if (target.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 128.0D * 128.0D) {
                target.sendMessage(Text.translatable("message.candyinfection.core_destroyed"), false);
            }
        }
        CandyLog.init("Infection core destroyed at " + pos.toShortString() + " - purging " + PURGE_RADIUS + " blocks");
    }

    private static void dropLoot(ServerWorld world, BlockPos pos, int stage) {
        List<ItemStack> drops = List.of(
                new ItemStack(CandyItems.INFECTION_CRYSTAL, 1 + stage),
                new ItemStack(CandyItems.CHOCOLATE_CORE, 1 + stage / 2),
                new ItemStack(CandyItems.CANDY_ESSENCE, 2 + stage),
                new ItemStack(CandyItems.PURIFICATION_CRYSTAL, 1),
                new ItemStack(CandyItems.HOLY_SUGAR, stage >= 3 ? 1 : 0));
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack));
        }
    }
}
