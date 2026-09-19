package dev.candyinfection.item;

import dev.candyinfection.init.CandyItems;
import dev.candyinfection.infection.InfectionConversions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Candy pickaxe: extra candy material from infected stone. */
public class CandyPickaxeItem extends PickaxeItem {
    public CandyPickaxeItem(ToolMaterial material, Settings settings) {
        super(material, CandyToolBridge.candyToolSettings(material, 1.0F, -2.8F));
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && InfectionConversions.isInfected(state)
                && serverWorld.random.nextFloat() < 0.3F) {
            CandyItems.dropInfectedBonus(serverWorld, pos);
        }
        return super.postMine(stack, world, state, pos, miner);
    }
}
