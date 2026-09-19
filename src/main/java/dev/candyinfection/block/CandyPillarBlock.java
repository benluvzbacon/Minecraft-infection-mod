package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.PillarBlock;

/** Infected wood: a gummy log that can be placed along any axis. */
public class CandyPillarBlock extends PillarBlock {
    public static final MapCodec<CandyPillarBlock> CODEC = createCodec(CandyPillarBlock::new);

    public CandyPillarBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }
}
