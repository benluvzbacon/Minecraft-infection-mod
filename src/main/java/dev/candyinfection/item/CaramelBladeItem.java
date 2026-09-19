package dev.candyinfection.item;

import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionSpread;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Caramel Blade: coats whatever it cuts in caramel. Hits apply Sticky, and
 * breaking an infected block with it has a chance to purify the block instead,
 * which makes it the go-to weapon for clearing a path through a colony.
 */
public class CaramelBladeItem extends SwordItem {
    public CaramelBladeItem(ToolMaterial material, Settings settings) {
        super(material, new Settings()
                .maxDamage(material.getDurability())
                .attributeModifiers(SwordItem.createAttributeModifiers(material, 5, -2.2F)));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addStatusEffect(new StatusEffectInstance(CandyEffects.STICKY, 160, 1, false, true));
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean postMine(ItemStack stack, net.minecraft.world.World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && InfectionConversions.isInfected(state)
                && serverWorld.random.nextFloat() < 0.35F) {
            net.minecraft.block.Block vanilla = InfectionConversions.vanillaForm(state.getBlock());
            if (vanilla != null) {
                world.setBlockState(pos, vanilla.getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
                dev.candyinfection.infection.InfectionWorldState.get(serverWorld).addChunkCount(pos, -1);
                dev.candyinfection.infection.InfectionWorldState.get(serverWorld).blockPurified();
                serverWorld.spawnParticles(dev.candyinfection.init.CandyParticles.PURIFICATION_SPARK,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }
}
