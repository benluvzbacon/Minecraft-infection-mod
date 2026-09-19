package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.block.entity.PurifierBlockEntity;
import dev.candyinfection.init.CandyBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Purifier: a machine that slowly scrubs the infection out of the blocks
 * around it. It needs purification crystals or holy sugar as fuel, which makes
 * reclaiming land a real cost rather than a free button.
 */
public class PurifierBlock extends BlockWithEntity {
    public static final MapCodec<PurifierBlock> CODEC = createCodec(PurifierBlock::new);

    public PurifierBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PurifierBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, CandyBlockEntities.PURIFIER, PurifierBlockEntity::serverTick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof PurifierBlockEntity purifier)) {
            return ActionResult.PASS;
        }
        ItemStack held = player.getStackInHand(Hand.MAIN_HAND);
        int fuel = PurifierBlockEntity.fuelValue(held);
        if (fuel > 0) {
            if (!player.getAbilities().creativeMode) {
                held.decrement(1);
            }
            purifier.addFuel(fuel);
            player.sendMessage(net.minecraft.text.Text.translatable("message.candyinfection.purifier_refuelled", fuel), true);
            return ActionResult.SUCCESS;
        }
        player.sendMessage(purifier.describe(), true);
        return ActionResult.CONSUME;
    }

}
