package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import dev.candyinfection.block.entity.InfectionCoreBlockEntity;
import dev.candyinfection.init.CandyBlockEntities;
import dev.candyinfection.init.CandyItems;
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
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The heart of an infection colony.
 *
 * <p>The core runs through four stages:
 * <ol>
 *   <li><b>Shielded</b> - the outer hard candy shell absorbs all damage.</li>
 *   <li><b>Cracked</b> - the shell breaks and the core starts spawning faster.</li>
 *   <li><b>Exposed</b> - the core is vulnerable and fights back harder.</li>
 *   <li><b>Critical</b> - the final phase: rapid spawning, heavy spreading.</li>
 * </ol>
 * Breaking it in the final stage triggers a purge: the surrounding infection
 * slowly dies back instead of being removed instantly, so the player still has
 * to fight through it.
 */
public class InfectionCoreBlock extends BlockWithEntity {
    public static final MapCodec<InfectionCoreBlock> CODEC = createCodec(InfectionCoreBlock::new);
    public static final IntProperty STAGE = IntProperty.of("stage", 1, 4);

    public InfectionCoreBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(STAGE, 1));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InfectionCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, CandyBlockEntities.INFECTION_CORE, InfectionCoreBlockEntity::serverTick);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient && world.getBlockEntity(pos) instanceof InfectionCoreBlockEntity core) {
            core.onPlaced();
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof InfectionCoreBlockEntity core) {
                core.onRemoved();
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            dev.candyinfection.infection.InfectionCoreLogic.onCoreDestroyed(world, pos, state, player);
        }
        return super.onBreak(world, pos, state, player);
    }

    /** Loot for the core comes from its loot table plus this bonus drop. */
    public static ItemStack coreDrop() {
        return new ItemStack(CandyItems.INFECTION_CRYSTAL, 3);
    }
}
