package dev.candyinfection.item;

import dev.candyinfection.infection.InfectionConversions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Candy hoe: tilling infected soil purifies it. This gives farmers a slow but
 * free way to reclaim farmland at the edge of a colony.
 */
public class CandyHoeItem extends HoeItem {
    public CandyHoeItem(ToolMaterial material, Settings settings) {
        super(material, CandyToolBridge.candyToolSettings(material, 0.0F, -2.0F));
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && InfectionConversions.isInfected(state)
                && serverWorld.random.nextFloat() < 0.5F) {
            net.minecraft.block.Block vanilla = InfectionConversions.vanillaForm(state.getBlock());
            if (vanilla != null) {
                world.setBlockState(pos, vanilla.getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
                dev.candyinfection.infection.InfectionWorldState.get(serverWorld).addChunkCount(pos, -1);
                dev.candyinfection.infection.InfectionWorldState.get(serverWorld).blockPurified();
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }
}
