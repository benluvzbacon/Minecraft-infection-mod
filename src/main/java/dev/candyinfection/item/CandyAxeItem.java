package dev.candyinfection.item;

import dev.candyinfection.init.CandyItems;
import dev.candyinfection.infection.InfectionConversions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Candy axe: shreds gummy logs and infected leaves for extra resin. */
public class CandyAxeItem extends AxeItem {
    public CandyAxeItem(ToolMaterial material, Settings settings) {
        super(material, CandyToolBridge.candyToolSettings(material, 5.0F, -3.0F));
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && InfectionConversions.isInfected(state)
                && serverWorld.random.nextFloat() < 0.4F) {
            CandyItems.dropInfectedBonus(serverWorld, pos);
        }
        return super.postMine(stack, world, state, pos, miner);
    }
}
