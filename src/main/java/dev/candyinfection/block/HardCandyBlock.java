package dev.candyinfection.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;

/**
 * Translucent hard candy. The building material of candy castles, fortresses
 * and the outer shell of an infection core.
 */
public class HardCandyBlock extends CandyBlock {
    public static final MapCodec<HardCandyBlock> CODEC = createCodec(HardCandyBlock::new);

    public HardCandyBlock(AbstractBlock.Settings settings) {
        super(settings.nonOpaque(), 0.4F);
    }

    @Override
    protected MapCodec<? extends AbstractBlock> getCodec() {
        return CODEC;
    }
}
