package dev.candyinfection.item;

import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Candy Hammer: slow but it breaks a 3x3 area, and it shatters infected blocks
 * into extra candy material.
 */
public class CandyHammerItem extends PickaxeItem {
    public CandyHammerItem(ToolMaterial material, Settings settings) {
        super(material, CandyToolBridge.candyToolSettings(material, 6.0F, -3.4F));
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && miner instanceof PlayerEntity player && !player.isSneaking()) {
            for (BlockPos candidate : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                if (candidate.equals(pos)) {
                    continue;
                }
                BlockState other = world.getBlockState(candidate);
                if (other.getHardness(world, candidate) <= 0.0F || other.getHardness(world, candidate) > 4.0F) {
                    continue;
                }
                world.breakBlock(candidate, true, player);
                stack.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
            }
            if (world instanceof ServerWorld serverWorld && InfectionConversions.isInfected(state)) {
                serverWorld.spawnParticles(CandyParticles.CHOCOLATE_FRAGMENT, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, 8, 0.4D, 0.4D, 0.4D, 0.04D);
                CandyItems.dropInfectedBonus(serverWorld, pos);
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }
}
